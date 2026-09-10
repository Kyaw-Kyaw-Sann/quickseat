package com.quickseat.dto.response.showtime;

import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.entity.enums.SeatType;
import java.math.BigDecimal;

public record ShowtimeSeatResponse(
        Long showtimeSeatId,
        Long seatId,
        String rowName,
        Integer seatNumber,
        SeatType seatType,
        BigDecimal price,
        SeatInventoryStatus status
) { }
