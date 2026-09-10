package com.quickseat.service.booking;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.request.booking.BookingCategory;
import com.quickseat.dto.response.booking.BookingResponse;
import com.quickseat.dto.response.booking.BookingSeatResponse;
import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.exception.BadRequestException;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.BookingRepository;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.ShowtimeSeatRepository;
import com.quickseat.security.CurrentUserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingManagementService {
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final CurrentUserService currentUserService;
    private final SeatHoldService seatHoldService;
    private final Clock clock;

    @Transactional
    public PageResponse<BookingResponse> list(BookingStatus status, BookingCategory category,
                                              LocalDate date, int page, int size) {
        User customer = currentUserService.getVerifiedCustomer();
        Instant now = clock.instant();
        seatHoldService.releaseExpiredHolds();

        Specification<Booking> specification = (root, query, builder) ->
                builder.equal(root.get("user").get("id"), customer.getId());
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (category != null) {
            specification = specification.and((root, query, builder) -> category == BookingCategory.UPCOMING
                    ? builder.greaterThan(root.get("showtime").get("startTime"), now)
                    : builder.lessThanOrEqualTo(root.get("showtime").get("startTime"), now));
        }
        if (date != null) {
            Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant nextDayStart = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            specification = specification.and((root, query, builder) -> builder.and(
                    builder.greaterThanOrEqualTo(root.get("showtime").get("startTime"), dayStart),
                    builder.lessThan(root.get("showtime").get("startTime"), nextDayStart)));
        }

        Page<Booking> bookings = bookingRepository.findAll(specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, List<BookingSeat>> seatsByBooking = loadSeatsByBooking(bookings.getContent());
        Page<BookingResponse> responsePage = bookings.map(booking ->
                toResponse(booking, seatsByBooking.getOrDefault(booking.getId(), List.of()), now));
        return PageResponse.from(responsePage);
    }

    @Transactional
    public BookingResponse get(String bookingReference) {
        User customer = currentUserService.getVerifiedCustomer();
        Booking booking = getCustomerBooking(bookingReference, customer.getId());
        Instant now = clock.instant();
        seatHoldService.expirePendingBookingIfNeeded(booking, now);
        List<BookingSeat> seats = bookingSeatRepository.findByBookingIdWithSeats(booking.getId());
        return toResponse(booking, seats, now);
    }

    @Transactional
    public BookingResponse cancel(String bookingReference) {
        User customer = currentUserService.getVerifiedCustomer();
        Booking booking = getCustomerBooking(bookingReference, customer.getId());
        Instant now = clock.instant();
        seatHoldService.expirePendingBookingIfNeeded(booking, now);
        List<BookingSeat> snapshots = bookingSeatRepository.findByBookingIdWithSeats(booking.getId());

        if (booking.getStatus() == BookingStatus.PENDING) {
            cancelPendingBooking(booking, snapshots);
        } else if (booking.getStatus() == BookingStatus.CONFIRMED) {
            cancelConfirmedBooking(booking, snapshots, now);
        } else {
            throw new ConflictException("Booking in " + booking.getStatus() + " status cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        log.info("Customer cancelled booking {}", booking.getId());
        return toResponse(booking, snapshots, now);
    }

    private void cancelPendingBooking(Booking booking, List<BookingSeat> snapshots) {
        List<ShowtimeSeat> heldSeats = showtimeSeatRepository
                .findHeldSeatsByBookingIdForUpdate(booking.getId());
        if (heldSeats.size() != snapshots.size()) {
            throw new ConflictException("Pending booking seat hold is inconsistent");
        }
        heldSeats.forEach(seatHoldService::releaseInventorySeat);
    }

    private void cancelConfirmedBooking(Booking booking, List<BookingSeat> snapshots, Instant now) {
        if (!booking.getShowtime().getStartTime().isAfter(now)) {
            throw new BadRequestException("Confirmed booking cannot be cancelled after the showtime starts");
        }
        List<Long> seatIds = snapshots.stream().map(snapshot -> snapshot.getSeat().getId()).toList();
        List<ShowtimeSeat> bookedSeats = seatIds.isEmpty() ? List.of()
                : showtimeSeatRepository.findBookingSeatsForUpdate(booking.getShowtime().getId(), seatIds);
        if (bookedSeats.size() != snapshots.size()
                || bookedSeats.stream().anyMatch(seat -> seat.getStatus() != SeatInventoryStatus.BOOKED)) {
            throw new ConflictException("Confirmed booking seat inventory is inconsistent");
        }
        bookedSeats.forEach(seatHoldService::releaseInventorySeat);
    }

    private Booking getCustomerBooking(String bookingReference, Long customerId) {
        return bookingRepository.findCustomerBookingForUpdate(bookingReference, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private Map<Long, List<BookingSeat>> loadSeatsByBooking(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> bookingIds = bookings.stream().map(Booking::getId).toList();
        return bookingSeatRepository.findByBookingIdsWithSeats(bookingIds).stream()
                .collect(Collectors.groupingBy(item -> item.getBooking().getId()));
    }

    private BookingResponse toResponse(Booking booking, List<BookingSeat> snapshots, Instant now) {
        List<BookingSeatResponse> seats = snapshots.stream()
                .map(this::toSeatResponse)
                .toList();
        BookingCategory category = booking.getShowtime().getStartTime().isAfter(now)
                ? BookingCategory.UPCOMING
                : BookingCategory.PAST;
        long remainingSeconds = booking.getStatus() == BookingStatus.PENDING && booking.getExpiresAt() != null
                ? Math.max(0, Duration.between(now, booking.getExpiresAt()).getSeconds())
                : 0;
        return new BookingResponse(booking.getId(), booking.getBookingReference(), booking.getStatus(), category,
                booking.getTotalAmount(), booking.getExpiresAt(), remainingSeconds, booking.getConfirmedAt(),
                booking.getCancelledAt(), booking.getCreatedAt(), booking.getShowtime().getId(),
                booking.getShowtime().getMovie().getTitle(), booking.getShowtime().getScreen().getCinema().getName(),
                booking.getShowtime().getScreen().getName(), booking.getShowtime().getStartTime(),
                booking.getShowtime().getEndTime(), seats);
    }

    private BookingSeatResponse toSeatResponse(BookingSeat snapshot) {
        return new BookingSeatResponse(snapshot.getSeat().getId(), snapshot.getSeat().getRowName(),
                snapshot.getSeat().getSeatNumber(), snapshot.getSeatType(), snapshot.getUnitPrice());
    }
}
