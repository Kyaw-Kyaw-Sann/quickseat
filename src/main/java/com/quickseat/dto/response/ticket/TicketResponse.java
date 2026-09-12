package com.quickseat.dto.response.ticket;

import com.quickseat.dto.response.booking.BookingSeatResponse;
import com.quickseat.entity.enums.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TicketResponse(
        Long ticketId,
        String ticketToken,
        TicketStatus status,
        String qrImageUrl,
        String bookingReference,
        BigDecimal totalAmount,
        String movieTitle,
        String cinemaName,
        String screenName,
        Instant startTime,
        Instant endTime,
        List<BookingSeatResponse> seats,
        Instant createdAt,
        boolean alreadyGenerated
) { }
