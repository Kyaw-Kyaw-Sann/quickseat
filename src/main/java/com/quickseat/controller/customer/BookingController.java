package com.quickseat.controller.customer;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.request.booking.BookingCategory;
import com.quickseat.dto.response.booking.BookingResponse;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.service.booking.BookingManagementService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/customer/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingManagementService bookingService;

    @GetMapping
    public ApiResponse<PageResponse<BookingResponse>> list(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) BookingCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Bookings retrieved successfully",
                bookingService.list(status, category, date, page, size));
    }

    @GetMapping("/{bookingReference}")
    public ApiResponse<BookingResponse> get(@PathVariable String bookingReference) {
        return ApiResponse.success("Booking retrieved successfully", bookingService.get(bookingReference));
    }

    @PatchMapping("/{bookingReference}/cancel")
    public ApiResponse<BookingResponse> cancel(@PathVariable String bookingReference) {
        return ApiResponse.success("Booking cancelled successfully", bookingService.cancel(bookingReference));
    }
}
