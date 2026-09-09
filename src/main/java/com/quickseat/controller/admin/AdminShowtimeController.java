package com.quickseat.controller.admin;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.request.showtime.ShowtimeRequest;
import com.quickseat.dto.response.showtime.ShowtimeResponse;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.service.showtime.ShowtimeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/showtimes")
@RequiredArgsConstructor
public class AdminShowtimeController {
    private final ShowtimeService showtimeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShowtimeResponse> create(@Valid @RequestBody ShowtimeRequest request) {
        return ApiResponse.success("Showtime created successfully", showtimeService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ShowtimeResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody ShowtimeRequest request) {
        return ApiResponse.success("Showtime updated successfully", showtimeService.update(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<ShowtimeResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success("Showtime cancelled successfully", showtimeService.cancel(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ShowtimeResponse>> list(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Showtimes retrieved successfully",
                showtimeService.list(movieId, cinemaId, date, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ShowtimeResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Showtime retrieved successfully", showtimeService.get(id));
    }
}
