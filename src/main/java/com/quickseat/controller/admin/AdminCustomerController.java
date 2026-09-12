package com.quickseat.controller.admin;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.request.common.ActiveStatusRequest;
import com.quickseat.dto.response.user.CustomerResponse;
import com.quickseat.service.user.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {
    private final AdminUserService userService;

    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Customers retrieved successfully",
                userService.listCustomers(search, active, emailVerified, page, size));
    }

    @GetMapping("/{userId}")
    public ApiResponse<CustomerResponse> get(@PathVariable Long userId) {
        return ApiResponse.success("Customer retrieved successfully", userService.getCustomer(userId));
    }

    @PatchMapping("/{userId}/active")
    public ApiResponse<CustomerResponse> setActive(@PathVariable Long userId,
                                                    @Valid @RequestBody ActiveStatusRequest request) {
        return ApiResponse.success("Customer active status updated successfully",
                userService.setCustomerActive(userId, request.active()));
    }
}
