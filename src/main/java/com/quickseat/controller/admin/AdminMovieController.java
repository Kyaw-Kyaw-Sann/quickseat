package com.quickseat.controller.admin;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.request.common.ActiveStatusRequest;
import com.quickseat.dto.request.movie.MovieRequest;
import com.quickseat.dto.request.movie.MovieStatusRequest;
import com.quickseat.dto.response.movie.ImageUploadResponse;
import com.quickseat.dto.response.movie.MovieResponse;
import com.quickseat.entity.enums.MovieStatus;
import com.quickseat.service.movie.MovieService;
import com.quickseat.service.shared.CloudinaryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/admin/movies")
@RequiredArgsConstructor
public class AdminMovieController {
    private final MovieService movieService;
    private final CloudinaryService cloudinaryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MovieResponse> create(@Valid @RequestBody MovieRequest request) {
        return ApiResponse.success("Movie created successfully", movieService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<MovieResponse> update(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        return ApiResponse.success("Movie updated successfully", movieService.update(id, request));
    }

    @GetMapping
    public ApiResponse<PageResponse<MovieResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Movies retrieved successfully",
                movieService.listAdmin(search, status, language, active, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<MovieResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Movie retrieved successfully", movieService.getAdmin(id));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<MovieResponse> setActive(@PathVariable Long id,
                                                 @Valid @RequestBody ActiveStatusRequest request) {
        return ApiResponse.success("Movie active status updated successfully",
                movieService.setActive(id, request.active()));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<MovieResponse> setStatus(@PathVariable Long id,
                                                 @Valid @RequestBody MovieStatusRequest request) {
        return ApiResponse.success("Movie status updated successfully",
                movieService.setStatus(id, request.status()));
    }

    @PostMapping(value = "/poster", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> uploadPoster(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Movie poster uploaded successfully", cloudinaryService.uploadMoviePoster(file));
    }
}
