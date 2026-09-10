package com.quickseat.dto.response.booking;

import com.quickseat.entity.enums.SeatType;
import java.math.BigDecimal;

public record HeldSeatResponse(
        Long showtimeSeatId,
        Long seatId,
        String rowName,
        Integer seatNumber,
        SeatType seatType,
        BigDecimal unitPrice
) { }
