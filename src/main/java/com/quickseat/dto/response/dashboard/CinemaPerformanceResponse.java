package com.quickseat.dto.response.dashboard;

import java.math.BigDecimal;

public record CinemaPerformanceResponse(
        Long cinemaId,
        String cinemaName,
        long totalBookings,
        long confirmedBookings,
        long cancelledBookings,
        BigDecimal revenue,
        long bookedSeatUnits,
        long capacitySeatUnits,
        BigDecimal occupancyRate
) {}
