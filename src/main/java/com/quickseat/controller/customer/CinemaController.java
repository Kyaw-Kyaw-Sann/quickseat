package com.quickseat.controller.customer;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.response.cinema.CinemaResponse;
import com.quickseat.service.cinema.CinemaDiscoveryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/cinemas")
@RequiredArgsConstructor
public class CinemaController {
    private final CinemaDiscoveryService cinemaDiscoveryService;

    @GetMapping
    public ApiResponse<PageResponse<CinemaResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Cinemas retrieved successfully",
                cinemaDiscoveryService.list(search, city, page, size));
    }

    @GetMapping("/{cinemaId}")
    public ApiResponse<CinemaResponse> get(@PathVariable @Positive Long cinemaId) {
        return ApiResponse.success("Cinema retrieved successfully", cinemaDiscoveryService.get(cinemaId));
    }
}
