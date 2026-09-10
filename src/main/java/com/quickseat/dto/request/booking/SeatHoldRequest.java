package com.quickseat.dto.request.booking;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SeatHoldRequest(
        @NotNull(message = "Showtime is required")
        @Positive(message = "Showtime ID must be positive")
        Long showtimeId,

        @NotEmpty(message = "At least one showtime seat is required")
        @Size(max = 20, message = "A maximum of 20 seats can be held at once")
        List<@NotNull(message = "Showtime seat ID is required")
                @Positive(message = "Showtime seat ID must be positive") Long> showtimeSeatIds
) { }
