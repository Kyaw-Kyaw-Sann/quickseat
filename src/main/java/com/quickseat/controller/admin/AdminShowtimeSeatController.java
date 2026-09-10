package com.quickseat.controller.admin;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.response.showtime.ShowtimeSeatMapResponse;
import com.quickseat.service.showtime.ShowtimeSeatInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/showtimes/{showtimeId}/seats")
@RequiredArgsConstructor
public class AdminShowtimeSeatController {
    private final ShowtimeSeatInventoryService inventoryService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShowtimeSeatMapResponse> generate(@PathVariable Long showtimeId) {
        return ApiResponse.success("Showtime seat inventory generated successfully",
                inventoryService.generate(showtimeId));
    }
}
