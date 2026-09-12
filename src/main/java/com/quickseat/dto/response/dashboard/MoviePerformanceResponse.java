package com.quickseat.dto.response.dashboard;

import java.math.BigDecimal;

public record MoviePerformanceResponse(
        Long movieId,
        String movieTitle,
        long totalBookings,
        long confirmedBookings,
        long cancelledBookings,
        BigDecimal revenue,
        long bookedSeatUnits,
        long capacitySeatUnits,
        BigDecimal occupancyRate
) {}
