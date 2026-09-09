package com.quickseat.controller.customer;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.response.movie.MovieResponse;
import com.quickseat.entity.enums.MovieStatus;
import com.quickseat.service.movie.MovieService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    @GetMapping
    public ApiResponse<PageResponse<MovieResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Movies retrieved successfully",
                movieService.listPublic(search, status, language, page, size));
    }

    @GetMapping("/now-showing")
    public ApiResponse<PageResponse<MovieResponse>> nowShowing(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Now-showing movies retrieved successfully",
                movieService.listPublic(search, MovieStatus.NOW_SHOWING, language, page, size));
    }

    @GetMapping("/upcoming")
    public ApiResponse<PageResponse<MovieResponse>> upcoming(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Upcoming movies retrieved successfully",
                movieService.listPublic(search, MovieStatus.UPCOMING, language, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<MovieResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Movie retrieved successfully", movieService.getPublic(id));
    }
}
