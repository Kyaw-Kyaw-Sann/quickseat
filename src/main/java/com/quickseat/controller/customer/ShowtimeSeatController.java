package com.quickseat.controller.customer;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.response.showtime.ShowtimeSeatMapResponse;
import com.quickseat.service.showtime.ShowtimeSeatInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/showtimes/{showtimeId}/seats")
@RequiredArgsConstructor
public class ShowtimeSeatController {
    private final ShowtimeSeatInventoryService inventoryService;

    @GetMapping
    public ApiResponse<ShowtimeSeatMapResponse> getSeatMap(@PathVariable Long showtimeId) {
        return ApiResponse.success("Showtime seat map retrieved successfully",
                inventoryService.getSeatMap(showtimeId));
    }
}
