package com.quickseat.dto.request.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MockPaymentRequest(
        @NotNull(message = "Payment result is required")
        Boolean successful,

        @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Payment amount must have at most 2 decimal places")
        BigDecimal amount
) { }
