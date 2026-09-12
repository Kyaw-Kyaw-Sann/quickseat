package com.quickseat.controller.customer;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.response.showtime.ShowtimeDiscoveryResponse;
import com.quickseat.service.showtime.ShowtimeDiscoveryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {
    private final ShowtimeDiscoveryService showtimeDiscoveryService;

    @GetMapping
    public ApiResponse<PageResponse<ShowtimeDiscoveryResponse>> list(
            @RequestParam(required = false) @Positive Long movieId,
            @RequestParam(required = false) @Positive Long cinemaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Showtimes retrieved successfully",
                showtimeDiscoveryService.list(movieId, cinemaId, date, page, size));
    }
}
