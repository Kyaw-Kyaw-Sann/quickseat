package com.quickseat.dto.response.showtime;

import java.time.Instant;
import java.util.List;

public record ShowtimeSeatMapResponse(
        Long showtimeId,
        String movieTitle,
        String screenName,
        Instant startTime,
        List<ShowtimeSeatResponse> seats
) { }
