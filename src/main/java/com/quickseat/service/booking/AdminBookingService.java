package com.quickseat.service.booking;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.response.booking.AdminBookingResponse;
import com.quickseat.dto.response.booking.BookingSeatResponse;
import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.Ticket;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.entity.enums.TicketStatus;
import com.quickseat.exception.BadRequestException;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.BookingRepository;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.ShowtimeSeatRepository;
import com.quickseat.repository.TicketRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
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
public class AdminBookingService {
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final TicketRepository ticketRepository;
    private final SeatHoldService seatHoldService;
    private final Clock clock;

    @Transactional
    public PageResponse<AdminBookingResponse> list(BookingStatus status, Long cinemaId, Long movieId,
                                                    Long showtimeId, LocalDate date, String search,
                                                    int page, int size) {
        seatHoldService.releaseExpiredHolds();
        Specification<Booking> specification = (root, query, builder) -> builder.conjunction();
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (cinemaId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("showtime").get("screen").get("cinema").get("id"), cinemaId));
        }
        if (movieId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("showtime").get("movie").get("id"), movieId));
        }
        if (showtimeId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("showtime").get("id"), showtimeId));
        }
        if (date != null) {
            Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant nextDayStart = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            specification = specification.and((root, query, builder) -> builder.and(
                    builder.greaterThanOrEqualTo(root.get("showtime").get("startTime"), dayStart),
                    builder.lessThan(root.get("showtime").get("startTime"), nextDayStart)));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("bookingReference")), pattern),
                    builder.like(builder.lower(root.get("user").get("email")), pattern)));
        }

        Page<Booking> bookings = bookingRepository.findAll(specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, List<BookingSeat>> seatsByBooking = loadSeatsByBooking(bookings.getContent());
        Map<Long, Ticket> ticketsByBooking = loadTicketsByBooking(bookings.getContent());
        Page<AdminBookingResponse> response = bookings.map(booking -> toResponse(booking,
                seatsByBooking.getOrDefault(booking.getId(), List.of()), ticketsByBooking.get(booking.getId())));
        return PageResponse.from(response);
    }

    @Transactional(readOnly = true)
    public AdminBookingResponse get(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        List<BookingSeat> seats = bookingSeatRepository.findByBookingIdWithSeats(booking.getId());
        Ticket ticket = ticketRepository.findByBookingId(booking.getId()).orElse(null);
        return toResponse(booking, seats, ticket);
    }

    @Transactional(noRollbackFor = ConflictException.class)
    public AdminBookingResponse cancel(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReferenceForUpdate(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        Instant now = clock.instant();
        seatHoldService.expirePendingBookingIfNeeded(booking, now);
        List<BookingSeat> snapshots = bookingSeatRepository.findByBookingIdWithSeats(booking.getId());
        Ticket ticket = ticketRepository.findByBookingId(booking.getId()).orElse(null);

        if (booking.getStatus() == BookingStatus.PENDING) {
            cancelPending(booking, snapshots);
        } else if (booking.getStatus() == BookingStatus.CONFIRMED) {
            cancelConfirmed(booking, snapshots, ticket, now);
        } else {
            throw new ConflictException("Booking in " + booking.getStatus() + " status cannot be cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        bookingRepository.flush();
        log.info("Admin cancelled booking {}", booking.getId());
        return toResponse(booking, snapshots, ticket);
    }

    private void cancelPending(Booking booking, List<BookingSeat> snapshots) {
        List<ShowtimeSeat> heldSeats = showtimeSeatRepository.findHeldSeatsByBookingIdForUpdate(booking.getId());
        if (heldSeats.size() != snapshots.size()) {
            throw new ConflictException("Pending booking seat hold is inconsistent");
        }
        heldSeats.forEach(seatHoldService::releaseInventorySeat);
    }

    private void cancelConfirmed(Booking booking, List<BookingSeat> snapshots, Ticket ticket, Instant now) {
        if (!booking.getShowtime().getStartTime().isAfter(now)) {
            throw new BadRequestException("Confirmed booking cannot be cancelled after the showtime starts");
        }
        List<Long> seatIds = snapshots.stream().map(item -> item.getSeat().getId()).toList();
        List<ShowtimeSeat> bookedSeats = seatIds.isEmpty() ? List.of()
                : showtimeSeatRepository.findBookingSeatsForUpdate(booking.getShowtime().getId(), seatIds);
        if (bookedSeats.size() != snapshots.size()
                || bookedSeats.stream().anyMatch(item -> item.getStatus() != SeatInventoryStatus.BOOKED)) {
            throw new ConflictException("Confirmed booking seat inventory is inconsistent");
        }
        if (ticket != null && ticket.getStatus() == TicketStatus.USED) {
            throw new ConflictException("Used ticket booking cannot be cancelled");
        }
        bookedSeats.forEach(seatHoldService::releaseInventorySeat);
        if (ticket != null) ticket.setStatus(TicketStatus.CANCELLED);
    }

    private Map<Long, List<BookingSeat>> loadSeatsByBooking(List<Booking> bookings) {
        if (bookings.isEmpty()) return Collections.emptyMap();
        return bookingSeatRepository.findByBookingIdsWithSeats(bookings.stream().map(Booking::getId).toList())
                .stream().collect(Collectors.groupingBy(item -> item.getBooking().getId()));
    }

    private Map<Long, Ticket> loadTicketsByBooking(List<Booking> bookings) {
        if (bookings.isEmpty()) return Collections.emptyMap();
        return ticketRepository.findByBookingIdIn(bookings.stream().map(Booking::getId).toList())
                .stream().collect(Collectors.toMap(item -> item.getBooking().getId(), Function.identity()));
    }

    private AdminBookingResponse toResponse(Booking booking, List<BookingSeat> seats, Ticket ticket) {
        List<BookingSeatResponse> seatResponses = seats.stream()
                .map(item -> new BookingSeatResponse(item.getSeat().getId(), item.getSeat().getRowName(),
                        item.getSeat().getSeatNumber(), item.getSeatType(), item.getUnitPrice()))
                .toList();
        return new AdminBookingResponse(booking.getId(), booking.getBookingReference(), booking.getStatus(),
                booking.getTotalAmount(), booking.getUser().getId(), booking.getUser().getName(),
                booking.getUser().getEmail(), booking.getShowtime().getId(), booking.getShowtime().getMovie().getId(),
                booking.getShowtime().getMovie().getTitle(), booking.getShowtime().getScreen().getCinema().getId(),
                booking.getShowtime().getScreen().getCinema().getName(), booking.getShowtime().getScreen().getName(),
                booking.getShowtime().getStartTime(), booking.getShowtime().getEndTime(), seatResponses,
                ticket == null ? null : ticket.getStatus(), booking.getConfirmedAt(), booking.getCancelledAt(),
                booking.getCreatedAt());
    }
}
