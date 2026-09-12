package com.quickseat.dto.response.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueTrendResponse(
        LocalDate periodStart,
        BigDecimal revenue,
        long confirmedBookings
) {}
