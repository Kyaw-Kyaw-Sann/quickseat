package com.quickseat.service.staff;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.response.booking.BookingSeatResponse;
import com.quickseat.dto.response.cinema.CinemaResponse;
import com.quickseat.dto.response.showtime.ShowtimeResponse;
import com.quickseat.dto.response.showtime.ShowtimeSeatMapResponse;
import com.quickseat.dto.response.showtime.ShowtimeSeatResponse;
import com.quickseat.dto.response.staff.StaffBookingResponse;
import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.Seat;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.exception.ForbiddenException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.BookingRepository;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.ShowtimeRepository;
import com.quickseat.repository.ShowtimeSeatRepository;
import com.quickseat.security.CurrentUserService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffOperationsService {
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public CinemaResponse getAssignedCinema() {
        return toCinemaResponse(currentUserService.getActiveStaffWithCinema().getCinema());
    }

    @Transactional(readOnly = true)
    public PageResponse<ShowtimeResponse> listShowtimes(Long movieId, LocalDate date,
                                                        ShowtimeStatus status, int page, int size) {
        User staff = currentUserService.getActiveStaffWithCinema();
        Specification<Showtime> specification = (root, query, builder) ->
                builder.equal(root.get("screen").get("cinema").get("id"), staff.getCinema().getId());
        if (movieId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("movie").get("id"), movieId));
        }
        if (date != null) {
            Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant nextDayStart = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            specification = specification.and((root, query, builder) -> builder.and(
                    builder.greaterThanOrEqualTo(root.get("startTime"), dayStart),
                    builder.lessThan(root.get("startTime"), nextDayStart)));
        }
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        Page<ShowtimeResponse> response = showtimeRepository.findAll(specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startTime")))
                .map(this::toShowtimeResponse);
        return PageResponse.from(response);
    }

    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtime(Long showtimeId) {
        User staff = currentUserService.getActiveStaffWithCinema();
        Showtime showtime = getShowtimeEntity(showtimeId);
        verifyCinemaAccess(staff, showtime.getScreen().getCinema().getId());
        return toShowtimeResponse(showtime);
    }

    @Transactional(readOnly = true)
    public ShowtimeSeatMapResponse getShowtimeSeats(Long showtimeId) {
        User staff = currentUserService.getActiveStaffWithCinema();
        Showtime showtime = getShowtimeEntity(showtimeId);
        verifyCinemaAccess(staff, showtime.getScreen().getCinema().getId());
        List<ShowtimeSeat> inventory = showtimeSeatRepository.findSeatMapByShowtimeId(showtimeId);
        if (inventory.isEmpty()) {
            throw new ResourceNotFoundException("Seat inventory has not been generated for this showtime");
        }
        List<ShowtimeSeatResponse> seats = inventory.stream()
                .map(item -> toSeatResponse(showtime, item))
                .toList();
        return new ShowtimeSeatMapResponse(showtime.getId(), showtime.getMovie().getTitle(),
                showtime.getScreen().getName(), showtime.getStartTime(), seats);
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffBookingResponse> listBookings(BookingStatus status, Long showtimeId,
                                                            LocalDate date, String search,
                                                            int page, int size) {
        User staff = currentUserService.getActiveStaffWithCinema();
        Specification<Booking> specification = (root, query, builder) ->
                builder.equal(root.get("showtime").get("screen").get("cinema").get("id"),
                        staff.getCinema().getId());
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
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
            String pattern = "%" + search.trim().toLowerCase() + "%";
            specification = specification.and((root, query, builder) ->
                    builder.like(builder.lower(root.get("bookingReference")), pattern));
        }

        Page<Booking> bookings = bookingRepository.findAll(specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, List<BookingSeat>> seatsByBooking = loadSeatsByBooking(bookings.getContent());
        Page<StaffBookingResponse> response = bookings.map(booking ->
                toBookingResponse(booking, seatsByBooking.getOrDefault(booking.getId(), List.of())));
        return PageResponse.from(response);
    }

    @Transactional(readOnly = true)
    public StaffBookingResponse getBooking(String bookingReference) {
        User staff = currentUserService.getActiveStaffWithCinema();
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        verifyCinemaAccess(staff, booking.getShowtime().getScreen().getCinema().getId());
        return toBookingResponse(booking, bookingSeatRepository.findByBookingIdWithSeats(booking.getId()));
    }

    private Showtime getShowtimeEntity(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
    }

    private void verifyCinemaAccess(User staff, Long resourceCinemaId) {
        if (!staff.getCinema().getId().equals(resourceCinemaId)) {
            throw new ForbiddenException("Staff cannot access another cinema's data");
        }
    }

    private Map<Long, List<BookingSeat>> loadSeatsByBooking(List<Booking> bookings) {
        if (bookings.isEmpty()) return Collections.emptyMap();
        return bookingSeatRepository.findByBookingIdsWithSeats(bookings.stream().map(Booking::getId).toList())
                .stream().collect(Collectors.groupingBy(item -> item.getBooking().getId()));
    }

    private CinemaResponse toCinemaResponse(Cinema cinema) {
        return new CinemaResponse(cinema.getId(), cinema.getName(), cinema.getAddress(), cinema.getCity(),
                cinema.getPhone(), cinema.getImageUrl(), cinema.isActive(), cinema.getCreatedAt(),
                cinema.getUpdatedAt());
    }

    private ShowtimeResponse toShowtimeResponse(Showtime showtime) {
        return new ShowtimeResponse(showtime.getId(), showtime.getMovie().getId(), showtime.getMovie().getTitle(),
                showtime.getScreen().getId(), showtime.getScreen().getName(),
                showtime.getScreen().getCinema().getId(), showtime.getScreen().getCinema().getName(),
                showtime.getStartTime(), showtime.getEndTime(), showtime.getCleaningBufferMinutes(),
                showtime.getNormalPrice(), showtime.getCouplePrice(), showtime.getStatus(),
                showtime.getCreatedAt(), showtime.getUpdatedAt());
    }

    private ShowtimeSeatResponse toSeatResponse(Showtime showtime, ShowtimeSeat item) {
        Seat seat = item.getSeat();
        BigDecimal price = seat.getSeatType() == SeatType.COUPLE
                ? showtime.getCouplePrice() : showtime.getNormalPrice();
        return new ShowtimeSeatResponse(item.getId(), seat.getId(), seat.getRowName(), seat.getSeatNumber(),
                seat.getSeatType(), price, item.getStatus());
    }

    private StaffBookingResponse toBookingResponse(Booking booking, List<BookingSeat> seats) {
        List<BookingSeatResponse> seatResponses = seats.stream()
                .map(item -> new BookingSeatResponse(item.getSeat().getId(), item.getSeat().getRowName(),
                        item.getSeat().getSeatNumber(), item.getSeatType(), item.getUnitPrice()))
                .toList();
        return new StaffBookingResponse(booking.getId(), booking.getBookingReference(), booking.getStatus(),
                booking.getTotalAmount(), booking.getUser().getId(), booking.getUser().getName(),
                booking.getUser().getEmail(), booking.getShowtime().getId(),
                booking.getShowtime().getMovie().getTitle(), booking.getShowtime().getScreen().getCinema().getId(),
                booking.getShowtime().getScreen().getCinema().getName(), booking.getShowtime().getScreen().getName(),
                booking.getShowtime().getStartTime(), booking.getShowtime().getEndTime(), seatResponses,
                booking.getConfirmedAt(), booking.getCancelledAt(), booking.getCreatedAt());
    }
}
