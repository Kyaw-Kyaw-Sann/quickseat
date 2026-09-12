package com.quickseat.service.booking;

import com.quickseat.dto.request.booking.SeatHoldRequest;
import com.quickseat.dto.response.booking.HeldSeatResponse;
import com.quickseat.dto.response.booking.SeatHoldResponse;
import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.Seat;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.exception.BadRequestException;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.BookingRepository;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.ShowtimeRepository;
import com.quickseat.repository.ShowtimeSeatRepository;
import com.quickseat.security.CurrentUserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SeatHoldService {
    private static final int REFERENCE_ATTEMPTS = 5;

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;
    private final long holdDurationMinutes;

    public SeatHoldService(BookingRepository bookingRepository,
                           BookingSeatRepository bookingSeatRepository,
                           ShowtimeRepository showtimeRepository,
                           ShowtimeSeatRepository showtimeSeatRepository,
                           CurrentUserService currentUserService,
                           Clock clock,
                           @Value("${app.booking.hold-duration-minutes:5}") long holdDurationMinutes) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.showtimeRepository = showtimeRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.currentUserService = currentUserService;
        this.clock = clock;
        if (holdDurationMinutes <= 0) {
            throw new IllegalArgumentException("Seat hold duration must be greater than zero");
        }
        this.holdDurationMinutes = holdDurationMinutes;
    }

    @Transactional
    public SeatHoldResponse hold(SeatHoldRequest request) {
        User customer = currentUserService.getVerifiedCustomer();
        Instant now = clock.instant();
        validateSeatIds(request.showtimeSeatIds());

        Showtime showtime = showtimeRepository.findById(request.showtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
        validateShowtime(showtime, now);

        List<Long> orderedIds = request.showtimeSeatIds().stream().sorted().toList();
        List<ShowtimeSeat> selectedSeats = showtimeSeatRepository
                .findSelectedSeatsForUpdate(showtime.getId(), orderedIds);
        if (selectedSeats.size() != orderedIds.size()) {
            throw new BadRequestException("One or more selected seats do not belong to this showtime");
        }

        releaseExpiredSelectedHolds(selectedSeats, now);
        validateAvailableSeats(selectedSeats);

        Instant expiresAt = now.plus(Duration.ofMinutes(holdDurationMinutes));
        BigDecimal totalAmount = selectedSeats.stream()
                .map(seat -> priceFor(showtime, seat.getSeat().getSeatType()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Booking booking = new Booking();
        booking.setBookingReference(generateBookingReference());
        booking.setUser(customer);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(totalAmount);
        booking.setExpiresAt(expiresAt);
        Booking savedBooking = bookingRepository.save(booking);

        List<BookingSeat> snapshots = selectedSeats.stream()
                .map(showtimeSeat -> createSnapshot(savedBooking, showtime, showtimeSeat.getSeat()))
                .toList();
        bookingSeatRepository.saveAll(snapshots);

        selectedSeats.forEach(showtimeSeat -> {
            showtimeSeat.setStatus(SeatInventoryStatus.HELD);
            showtimeSeat.setHeldByBooking(savedBooking);
            showtimeSeat.setHoldExpiresAt(expiresAt);
        });

        log.info("Created pending booking {} holding {} seats", savedBooking.getId(), selectedSeats.size());
        return toResponse(savedBooking, snapshots, selectedSeats, now);
    }

    @Transactional
    public SeatHoldResponse get(String bookingReference) {
        User customer = currentUserService.getVerifiedCustomer();
        Booking booking = getCustomerBookingForUpdate(bookingReference, customer.getId());
        Instant now = clock.instant();
        List<BookingSeat> snapshots = bookingSeatRepository.findByBookingIdWithSeats(booking.getId());

        List<ShowtimeSeat> inventory;
        if (isExpiredPendingBooking(booking, now)) {
            inventory = showtimeSeatRepository.findHeldSeatsByBookingIdForUpdate(booking.getId());
            expireBookingAndRelease(booking, inventory);
        } else {
            inventory = findInventoryForSnapshots(booking, snapshots);
        }
        return toResponse(booking, snapshots, inventory, now);
    }

    @Transactional
    public SeatHoldResponse release(String bookingReference) {
        User customer = currentUserService.getVerifiedCustomer();
        Booking booking = getCustomerBookingForUpdate(bookingReference, customer.getId());
        Instant now = clock.instant();
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only a pending seat hold can be released");
        }

        List<BookingSeat> snapshots = bookingSeatRepository.findByBookingIdWithSeats(booking.getId());
        List<ShowtimeSeat> heldSeats = showtimeSeatRepository.findHeldSeatsByBookingIdForUpdate(booking.getId());
        if (isExpiredPendingBooking(booking, now)) {
            expireBookingAndRelease(booking, heldSeats);
        } else {
            heldSeats.forEach(this::releaseInventorySeat);
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(now);
            log.info("Released seat hold for booking {}", booking.getId());
        }
        return toResponse(booking, snapshots, heldSeats, now);
    }

    @Scheduled(fixedDelayString = "${app.booking.hold-cleanup-interval-ms:60000}")
    @Transactional
    public void releaseExpiredHolds() {
        Instant now = clock.instant();
        List<ShowtimeSeat> expiredSeats = showtimeSeatRepository
                .findExpiredHoldsForUpdate(SeatInventoryStatus.HELD, now);
        Set<Long> expiredBookingIds = new HashSet<>();
        for (ShowtimeSeat showtimeSeat : expiredSeats) {
            Booking booking = showtimeSeat.getHeldByBooking();
            if (booking != null && booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.EXPIRED);
                expiredBookingIds.add(booking.getId());
            }
            releaseInventorySeat(showtimeSeat);
        }
        if (!expiredSeats.isEmpty()) {
            log.info("Released {} expired seat holds across {} bookings",
                    expiredSeats.size(), expiredBookingIds.size());
        }
    }

    private void validateSeatIds(List<Long> showtimeSeatIds) {
        if (showtimeSeatIds == null || showtimeSeatIds.isEmpty()) {
            throw new BadRequestException("At least one showtime seat is required");
        }
        if (new HashSet<>(showtimeSeatIds).size() != showtimeSeatIds.size()) {
            throw new BadRequestException("Duplicate showtime seat IDs are not allowed");
        }
    }

    private void validateShowtime(Showtime showtime, Instant now) {
        if (showtime.getStatus() != ShowtimeStatus.ACTIVE) {
            throw new BadRequestException("Seats can only be held for an active showtime");
        }
        if (!showtime.getStartTime().isAfter(now)) {
            throw new BadRequestException("Seats cannot be held for a past or started showtime");
        }
        if (!showtime.getMovie().isActive() || !showtime.getScreen().isActive()
                || !showtime.getScreen().getCinema().isActive()) {
            throw new BadRequestException("Showtime is not currently available for booking");
        }
    }

    private void releaseExpiredSelectedHolds(List<ShowtimeSeat> selectedSeats, Instant now) {
        Set<Long> processedBookings = new HashSet<>();
        for (ShowtimeSeat selectedSeat : selectedSeats) {
            if (selectedSeat.getStatus() == SeatInventoryStatus.HELD
                    && selectedSeat.getHoldExpiresAt() != null
                    && !selectedSeat.getHoldExpiresAt().isAfter(now)) {
                Booking expiredBooking = selectedSeat.getHeldByBooking();
                if (expiredBooking != null && processedBookings.add(expiredBooking.getId())) {
                    List<ShowtimeSeat> bookingSeats = showtimeSeatRepository
                            .findHeldSeatsByBookingIdForUpdate(expiredBooking.getId());
                    expireBookingAndRelease(expiredBooking, bookingSeats);
                }
                releaseInventorySeat(selectedSeat);
            }
        }
    }

    private void validateAvailableSeats(List<ShowtimeSeat> selectedSeats) {
        for (ShowtimeSeat showtimeSeat : selectedSeats) {
            if (!showtimeSeat.getSeat().isActive()) {
                throw new ConflictException("An inactive seat cannot be held");
            }
            if (showtimeSeat.getStatus() != SeatInventoryStatus.AVAILABLE) {
                throw new ConflictException("One or more selected seats are not available");
            }
        }
    }

    private BookingSeat createSnapshot(Booking booking, Showtime showtime, Seat seat) {
        BookingSeat bookingSeat = new BookingSeat();
        bookingSeat.setBooking(booking);
        bookingSeat.setSeat(seat);
        bookingSeat.setSeatType(seat.getSeatType());
        bookingSeat.setUnitPrice(priceFor(showtime, seat.getSeatType()));
        return bookingSeat;
    }

    private BigDecimal priceFor(Showtime showtime, SeatType seatType) {
        return seatType == SeatType.COUPLE ? showtime.getCouplePrice() : showtime.getNormalPrice();
    }

    private void expireBookingAndRelease(Booking booking, List<ShowtimeSeat> heldSeats) {
        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.EXPIRED);
        }
        heldSeats.forEach(this::releaseInventorySeat);
    }

    public boolean expirePendingBookingIfNeeded(Booking booking, Instant now) {
        if (!isExpiredPendingBooking(booking, now)) {
            return false;
        }
        List<ShowtimeSeat> heldSeats = showtimeSeatRepository
                .findHeldSeatsByBookingIdForUpdate(booking.getId());
        expireBookingAndRelease(booking, heldSeats);
        return true;
    }

    public void expirePendingBooking(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }
        List<ShowtimeSeat> heldSeats = showtimeSeatRepository
                .findHeldSeatsByBookingIdForUpdate(booking.getId());
        expireBookingAndRelease(booking, heldSeats);
    }

    public void releaseInventorySeat(ShowtimeSeat showtimeSeat) {
        showtimeSeat.setStatus(showtimeSeat.getSeat().isActive()
                ? SeatInventoryStatus.AVAILABLE
                : SeatInventoryStatus.UNAVAILABLE);
        showtimeSeat.setHeldByBooking(null);
        showtimeSeat.setHoldExpiresAt(null);
    }

    private boolean isExpiredPendingBooking(Booking booking, Instant now) {
        return booking.getStatus() == BookingStatus.PENDING
                && booking.getExpiresAt() != null
                && !booking.getExpiresAt().isAfter(now);
    }

    private Booking getCustomerBookingForUpdate(String bookingReference, Long customerId) {
        return bookingRepository.findCustomerBookingForUpdate(bookingReference, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat hold not found"));
    }

    private List<ShowtimeSeat> findInventoryForSnapshots(Booking booking, List<BookingSeat> snapshots) {
        List<Long> seatIds = snapshots.stream().map(snapshot -> snapshot.getSeat().getId()).toList();
        if (seatIds.isEmpty()) {
            return List.of();
        }
        return showtimeSeatRepository.findByShowtimeIdAndSeatIds(booking.getShowtime().getId(), seatIds);
    }

    private SeatHoldResponse toResponse(Booking booking, List<BookingSeat> snapshots,
                                        List<ShowtimeSeat> inventory, Instant now) {
        Map<Long, ShowtimeSeat> inventoryBySeatId = new HashMap<>();
        inventory.forEach(showtimeSeat -> inventoryBySeatId.put(showtimeSeat.getSeat().getId(), showtimeSeat));

        List<BookingSeat> orderedSnapshots = new ArrayList<>(snapshots);
        orderedSnapshots.sort(Comparator.comparing((BookingSeat item) -> item.getSeat().getRowName())
                .thenComparing(item -> item.getSeat().getSeatNumber()));
        List<HeldSeatResponse> selectedSeats = orderedSnapshots.stream()
                .map(snapshot -> {
                    ShowtimeSeat showtimeSeat = inventoryBySeatId.get(snapshot.getSeat().getId());
                    if (showtimeSeat == null) {
                        throw new ResourceNotFoundException("Showtime seat inventory no longer exists");
                    }
                    Seat seat = snapshot.getSeat();
                    return new HeldSeatResponse(showtimeSeat.getId(), seat.getId(), seat.getRowName(),
                            seat.getSeatNumber(), snapshot.getSeatType(), snapshot.getUnitPrice());
                })
                .toList();
        long remainingSeconds = booking.getStatus() == BookingStatus.PENDING && booking.getExpiresAt() != null
                ? Math.max(0, Duration.between(now, booking.getExpiresAt()).getSeconds())
                : 0;
        return new SeatHoldResponse(booking.getId(), booking.getBookingReference(), booking.getStatus(),
                booking.getTotalAmount(), booking.getExpiresAt(), remainingSeconds, selectedSeats);
    }

    private String generateBookingReference() {
        for (int attempt = 0; attempt < REFERENCE_ATTEMPTS; attempt++) {
            String reference = "QS-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12).toUpperCase();
            if (!bookingRepository.existsByBookingReference(reference)) {
                return reference;
            }
        }
        throw new ConflictException("Could not generate a unique booking reference");
    }
}
