package com.quickseat.controller.customer;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.request.booking.SeatHoldRequest;
import com.quickseat.dto.response.booking.SeatHoldResponse;
import com.quickseat.service.booking.SeatHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer/seat-holds")
@RequiredArgsConstructor
public class SeatHoldController {
    private final SeatHoldService seatHoldService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SeatHoldResponse> hold(@Valid @RequestBody SeatHoldRequest request) {
        return ApiResponse.success("Seats held successfully", seatHoldService.hold(request));
    }

    @GetMapping("/{bookingReference}")
    public ApiResponse<SeatHoldResponse> get(@PathVariable String bookingReference) {
        return ApiResponse.success("Seat hold retrieved successfully", seatHoldService.get(bookingReference));
    }

    @DeleteMapping("/{bookingReference}")
    public ApiResponse<SeatHoldResponse> release(@PathVariable String bookingReference) {
        return ApiResponse.success("Seat hold released successfully", seatHoldService.release(bookingReference));
    }
}
