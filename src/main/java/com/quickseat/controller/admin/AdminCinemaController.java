package com.quickseat.controller.admin;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.request.cinema.CinemaRequest;
import com.quickseat.dto.request.common.ActiveStatusRequest;
import com.quickseat.dto.response.cinema.CinemaResponse;
import com.quickseat.dto.response.movie.ImageUploadResponse;
import com.quickseat.service.cinema.CinemaService;
import com.quickseat.service.shared.CloudinaryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/cinemas")
@RequiredArgsConstructor
@Validated
public class AdminCinemaController {
    private final CinemaService cinemaService;
    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> uploadImage(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Cinema image uploaded successfully", cloudinaryService.uploadCinemaImage(file));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CinemaResponse> create(@Valid @RequestBody CinemaRequest request) {
        return ApiResponse.success("Cinema created successfully", cinemaService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CinemaResponse> update(@PathVariable Long id, @Valid @RequestBody CinemaRequest request) {
        return ApiResponse.success("Cinema updated successfully", cinemaService.update(id, request));
    }

    @GetMapping
    public ApiResponse<Page<CinemaResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Cinemas retrieved successfully", cinemaService.list(search, active, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CinemaResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Cinema retrieved successfully", cinemaService.get(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<CinemaResponse> setActive(@PathVariable Long id, @Valid @RequestBody ActiveStatusRequest request) {
        return ApiResponse.success("Cinema status updated successfully", cinemaService.setActive(id, request.active()));
    }
}
