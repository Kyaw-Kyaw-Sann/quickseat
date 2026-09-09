package com.quickseat.dto.request.showtime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record ShowtimeRequest(
        @NotNull Long movieId,
        @NotNull Long screenId,
        @NotNull @Future Instant startTime,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal normalPrice,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal couplePrice,
        @NotNull @Min(0) @Max(180) Integer cleaningBufferMinutes
) { }
