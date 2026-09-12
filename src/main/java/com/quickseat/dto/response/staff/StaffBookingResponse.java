package com.quickseat.dto.response.staff;

import com.quickseat.dto.response.booking.BookingSeatResponse;
import com.quickseat.entity.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StaffBookingResponse(
        Long bookingId,
        String bookingReference,
        BookingStatus status,
        BigDecimal totalAmount,
        Long customerId,
        String customerName,
        String customerEmail,
        Long showtimeId,
        String movieTitle,
        Long cinemaId,
        String cinemaName,
        String screenName,
        Instant startTime,
        Instant endTime,
        List<BookingSeatResponse> seats,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant createdAt
) { }
