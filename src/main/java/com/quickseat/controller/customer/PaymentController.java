package com.quickseat.controller.customer;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.request.payment.MockPaymentRequest;
import com.quickseat.dto.response.payment.PaymentResponse;
import com.quickseat.dto.response.payment.PaymentSummaryResponse;
import com.quickseat.service.payment.MockPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer/bookings/{bookingReference}")
@RequiredArgsConstructor
public class PaymentController {
    private final MockPaymentService paymentService;

    @GetMapping("/payment-summary")
    public ApiResponse<PaymentSummaryResponse> getSummary(@PathVariable String bookingReference) {
        return ApiResponse.success("Payment summary retrieved successfully",
                paymentService.getSummary(bookingReference));
    }

    @PostMapping("/payments")
    public ApiResponse<PaymentResponse> process(@PathVariable String bookingReference,
                                                @Valid @RequestBody MockPaymentRequest request) {
        return ApiResponse.success(request.successful() ? "Mock payment completed successfully"
                        : "Mock payment failed", paymentService.process(bookingReference, request));
    }
}
