package com.quickseat.dto.response.staff;

import com.quickseat.dto.response.booking.BookingSeatResponse;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StaffTicketResponse(
        Long ticketId,
        String ticketToken,
        TicketStatus ticketStatus,
        String bookingReference,
        BookingStatus bookingStatus,
        BigDecimal totalAmount,
        Long customerId,
        String customerName,
        String movieTitle,
        String cinemaName,
        String screenName,
        Instant startTime,
        List<BookingSeatResponse> seats,
        Instant usedAt,
        Long verifiedByStaffId
) { }
