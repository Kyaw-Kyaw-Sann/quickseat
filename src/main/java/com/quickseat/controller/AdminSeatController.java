package com.quickseat.controller;

import com.quickseat.dto.ActiveStatusRequest;
import com.quickseat.dto.ApiResponse;
import com.quickseat.dto.SeatLayoutRequest;
import com.quickseat.dto.SeatResponse;
import com.quickseat.dto.SeatUpdateRequest;
import com.quickseat.service.SeatService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSeatController {
    private final SeatService seatService;

    @PostMapping("/screens/{screenId}/seats/layout")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<SeatResponse>> generateLayout(@PathVariable Long screenId,
                                                           @Valid @RequestBody SeatLayoutRequest request) {
        return ApiResponse.success("Seat layout generated successfully", seatService.generateLayout(screenId, request));
    }

    @GetMapping("/screens/{screenId}/seats")
    public ApiResponse<List<SeatResponse>> list(@PathVariable Long screenId) {
        return ApiResponse.success("Seats retrieved successfully", seatService.list(screenId));
    }

    @PutMapping("/seats/{id}")
    public ApiResponse<SeatResponse> update(@PathVariable Long id, @Valid @RequestBody SeatUpdateRequest request) {
        return ApiResponse.success("Seat updated successfully", seatService.update(id, request));
    }

    @PatchMapping("/seats/{id}/status")
    public ApiResponse<SeatResponse> setActive(@PathVariable Long id, @Valid @RequestBody ActiveStatusRequest request) {
        return ApiResponse.success("Seat status updated successfully", seatService.setActive(id, request.active()));
    }
}
