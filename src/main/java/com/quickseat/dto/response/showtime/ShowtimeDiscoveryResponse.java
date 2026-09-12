package com.quickseat.dto.response.showtime;

import java.math.BigDecimal;
import java.time.Instant;

public record ShowtimeDiscoveryResponse(
        Long showtimeId,
        Long movieId,
        String movieTitle,
        Long cinemaId,
        String cinemaName,
        Long screenId,
        String screenName,
        Instant startTime,
        Instant endTime,
        BigDecimal normalPrice,
        BigDecimal couplePrice
) { }
