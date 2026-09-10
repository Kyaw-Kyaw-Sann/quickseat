package com.quickseat.dto.response.booking;

import com.quickseat.dto.request.booking.BookingCategory;
import com.quickseat.entity.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingResponse(
        Long bookingId,
        String bookingReference,
        BookingStatus status,
        BookingCategory category,
        BigDecimal totalAmount,
        Instant expiresAt,
        long remainingSeconds,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant createdAt,
        Long showtimeId,
        String movieTitle,
        String cinemaName,
        String screenName,
        Instant startTime,
        Instant endTime,
        List<BookingSeatResponse> seats
) { }
