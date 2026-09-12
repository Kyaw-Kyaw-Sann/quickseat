package com.quickseat.dto.response.payment;

import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long paymentId,
        String paymentReference,
        PaymentStatus paymentStatus,
        BigDecimal amount,
        Instant paidAt,
        String bookingReference,
        BookingStatus bookingStatus,
        Instant confirmedAt,
        boolean alreadyProcessed
) { }
