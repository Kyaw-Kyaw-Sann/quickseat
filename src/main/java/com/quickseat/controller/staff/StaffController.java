package com.quickseat.controller.staff;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.response.cinema.CinemaResponse;
import com.quickseat.dto.response.showtime.ShowtimeResponse;
import com.quickseat.dto.response.showtime.ShowtimeSeatMapResponse;
import com.quickseat.dto.response.staff.StaffBookingResponse;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.service.staff.StaffOperationsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {
    private final StaffOperationsService staffService;

    @GetMapping("/cinema")
    public ApiResponse<CinemaResponse> getCinema() {
        return ApiResponse.success("Assigned cinema retrieved successfully", staffService.getAssignedCinema());
    }

    @GetMapping("/showtimes")
    public ApiResponse<PageResponse<ShowtimeResponse>> listShowtimes(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Cinema showtimes retrieved successfully",
                staffService.listShowtimes(movieId, date, status, page, size));
    }

    @GetMapping("/showtimes/{showtimeId}")
    public ApiResponse<ShowtimeResponse> getShowtime(@PathVariable Long showtimeId) {
        return ApiResponse.success("Showtime retrieved successfully", staffService.getShowtime(showtimeId));
    }

    @GetMapping("/showtimes/{showtimeId}/seats")
    public ApiResponse<ShowtimeSeatMapResponse> getShowtimeSeats(@PathVariable Long showtimeId) {
        return ApiResponse.success("Showtime seat status retrieved successfully",
                staffService.getShowtimeSeats(showtimeId));
    }

    @GetMapping("/bookings")
    public ApiResponse<PageResponse<StaffBookingResponse>> listBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long showtimeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Cinema bookings retrieved successfully",
                staffService.listBookings(status, showtimeId, date, search, page, size));
    }

    @GetMapping("/bookings/{bookingReference}")
    public ApiResponse<StaffBookingResponse> getBooking(@PathVariable String bookingReference) {
        return ApiResponse.success("Booking retrieved successfully", staffService.getBooking(bookingReference));
    }
}
