package com.quickseat.controller.admin;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.response.dashboard.CinemaPerformanceResponse;
import com.quickseat.dto.response.dashboard.DashboardSummaryResponse;
import com.quickseat.dto.response.dashboard.MoviePerformanceResponse;
import com.quickseat.dto.response.dashboard.RevenueTrendResponse;
import com.quickseat.service.dashboard.DashboardService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Long cinemaId,
            @RequestParam(required = false) @Positive Long movieId) {
        return ApiResponse.success("Dashboard summary retrieved successfully",
                dashboardService.getSummary(from, to, cinemaId, movieId));
    }

    @GetMapping("/movies")
    public ApiResponse<List<MoviePerformanceResponse>> movies(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Long cinemaId,
            @RequestParam(required = false) @Positive Long movieId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return ApiResponse.success("Movie performance retrieved successfully",
                dashboardService.getMoviePerformance(from, to, cinemaId, movieId, limit));
    }

    @GetMapping("/movies/top")
    public ApiResponse<List<MoviePerformanceResponse>> mostBookedMovies(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Long cinemaId,
            @RequestParam(required = false) @Positive Long movieId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ApiResponse.success("Most-booked movies retrieved successfully",
                dashboardService.getMostBookedMovies(from, to, cinemaId, movieId, limit));
    }

    @GetMapping("/cinemas")
    public ApiResponse<List<CinemaPerformanceResponse>> cinemas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Long cinemaId,
            @RequestParam(required = false) @Positive Long movieId) {
        return ApiResponse.success("Cinema performance retrieved successfully",
                dashboardService.getCinemaPerformance(from, to, cinemaId, movieId));
    }

    @GetMapping("/revenue/daily")
    public ApiResponse<List<RevenueTrendResponse>> dailyRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Long cinemaId,
            @RequestParam(required = false) @Positive Long movieId) {
        return ApiResponse.success("Daily revenue trend retrieved successfully",
                dashboardService.getRevenueTrend(from, to, cinemaId, movieId, false));
    }

    @GetMapping("/revenue/monthly")
    public ApiResponse<List<RevenueTrendResponse>> monthlyRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Long cinemaId,
            @RequestParam(required = false) @Positive Long movieId) {
        return ApiResponse.success("Monthly revenue trend retrieved successfully",
                dashboardService.getRevenueTrend(from, to, cinemaId, movieId, true));
    }
}
