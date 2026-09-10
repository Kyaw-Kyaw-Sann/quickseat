package com.quickseat.dto.response.booking;

import com.quickseat.entity.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SeatHoldResponse(
        Long bookingId,
        String bookingReference,
        BookingStatus status,
        BigDecimal totalAmount,
        Instant expiresAt,
        long remainingSeconds,
        List<HeldSeatResponse> selectedSeats
) { }
