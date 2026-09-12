package com.quickseat.dto.response.dashboard;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long totalBookings,
        long confirmedBookings,
        long todayBookings,
        BigDecimal totalRevenue,
        BigDecimal occupancyRate,
        BigDecimal cancellationRate
) {}
