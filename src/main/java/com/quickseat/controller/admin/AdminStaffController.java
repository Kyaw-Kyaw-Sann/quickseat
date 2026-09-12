package com.quickseat.controller.admin;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.request.common.ActiveStatusRequest;
import com.quickseat.dto.request.user.StaffCinemaAssignmentRequest;
import com.quickseat.dto.request.user.StaffCreateRequest;
import com.quickseat.dto.request.user.StaffUpdateRequest;
import com.quickseat.dto.response.user.StaffResponse;
import com.quickseat.service.user.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/staff")
@RequiredArgsConstructor
public class AdminStaffController {
    private final AdminUserService userService;

    @PostMapping
    public ApiResponse<StaffResponse> create(@Valid @RequestBody StaffCreateRequest request) {
        return ApiResponse.success("Staff account created successfully", userService.createStaff(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<StaffResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Staff accounts retrieved successfully",
                userService.listStaff(search, active, cinemaId, page, size));
    }

    @GetMapping("/{staffId}")
    public ApiResponse<StaffResponse> get(@PathVariable Long staffId) {
        return ApiResponse.success("Staff account retrieved successfully", userService.getStaff(staffId));
    }

    @PutMapping("/{staffId}")
    public ApiResponse<StaffResponse> update(@PathVariable Long staffId,
                                             @Valid @RequestBody StaffUpdateRequest request) {
        return ApiResponse.success("Staff account updated successfully", userService.updateStaff(staffId, request));
    }

    @PatchMapping("/{staffId}/active")
    public ApiResponse<StaffResponse> setActive(@PathVariable Long staffId,
                                                @Valid @RequestBody ActiveStatusRequest request) {
        return ApiResponse.success("Staff active status updated successfully",
                userService.setStaffActive(staffId, request.active()));
    }

    @PatchMapping("/{staffId}/cinema")
    public ApiResponse<StaffResponse> assignCinema(@PathVariable Long staffId,
                                                   @Valid @RequestBody StaffCinemaAssignmentRequest request) {
        return ApiResponse.success("Staff cinema assignment updated successfully",
                userService.assignCinema(staffId, request.cinemaId()));
    }
}
