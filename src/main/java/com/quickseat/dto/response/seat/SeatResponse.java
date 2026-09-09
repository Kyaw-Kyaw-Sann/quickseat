package com.quickseat.dto.response.seat;

import com.quickseat.entity.enums.SeatType;
import java.time.Instant;

public record SeatResponse(Long id, Long screenId, String rowName, Integer seatNumber,
                           SeatType seatType, boolean active, Instant createdAt, Instant updatedAt) { }
