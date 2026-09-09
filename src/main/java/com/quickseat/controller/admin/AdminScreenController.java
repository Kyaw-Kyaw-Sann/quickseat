package com.quickseat.controller.admin;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.request.common.ActiveStatusRequest;
import com.quickseat.dto.request.screen.ScreenRequest;
import com.quickseat.dto.response.screen.ScreenResponse;
import com.quickseat.service.screen.ScreenService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminScreenController {
    private final ScreenService screenService;

    @PostMapping("/cinemas/{cinemaId}/screens")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScreenResponse> create(@PathVariable Long cinemaId, @Valid @RequestBody ScreenRequest request) {
        return ApiResponse.success("Screen created successfully", screenService.create(cinemaId, request));
    }

    @GetMapping("/cinemas/{cinemaId}/screens")
    public ApiResponse<List<ScreenResponse>> list(@PathVariable Long cinemaId) {
        return ApiResponse.success("Screens retrieved successfully", screenService.list(cinemaId));
    }

    @PutMapping("/screens/{id}")
    public ApiResponse<ScreenResponse> update(@PathVariable Long id, @Valid @RequestBody ScreenRequest request) {
        return ApiResponse.success("Screen updated successfully", screenService.update(id, request));
    }

    @PatchMapping("/screens/{id}/status")
    public ApiResponse<ScreenResponse> setActive(@PathVariable Long id, @Valid @RequestBody ActiveStatusRequest request) {
        return ApiResponse.success("Screen status updated successfully", screenService.setActive(id, request.active()));
    }
}
