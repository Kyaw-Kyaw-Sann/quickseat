package com.quickseat.dto.response.booking;

import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminBookingResponse(
        Long bookingId,
        String bookingReference,
        BookingStatus status,
        BigDecimal totalAmount,
        Long customerId,
        String customerName,
        String customerEmail,
        Long showtimeId,
        Long movieId,
        String movieTitle,
        Long cinemaId,
        String cinemaName,
        String screenName,
        Instant startTime,
        Instant endTime,
        List<BookingSeatResponse> seats,
        TicketStatus ticketStatus,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant createdAt
) { }
