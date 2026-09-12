package com.quickseat.dto.response.payment;

import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSummaryResponse(
        String bookingReference,
        BookingStatus bookingStatus,
        BigDecimal totalAmount,
        Instant expiresAt,
        long remainingSeconds,
        String paymentReference,
        PaymentStatus paymentStatus,
        boolean canPay
) { }
