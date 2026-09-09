package com.quickseat.dto.response.showtime;

import com.quickseat.entity.enums.ShowtimeStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ShowtimeResponse(
        Long id,
        Long movieId,
        String movieTitle,
        Long screenId,
        String screenName,
        Long cinemaId,
        String cinemaName,
        Instant startTime,
        Instant endTime,
        Integer cleaningBufferMinutes,
        BigDecimal normalPrice,
        BigDecimal couplePrice,
        ShowtimeStatus status,
        Instant createdAt,
        Instant updatedAt
) { }
