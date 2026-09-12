package com.quickseat.service.dashboard;

import com.quickseat.dto.response.dashboard.CinemaPerformanceResponse;
import com.quickseat.dto.response.dashboard.DashboardSummaryResponse;
import com.quickseat.dto.response.dashboard.MoviePerformanceResponse;
import com.quickseat.dto.response.dashboard.RevenueTrendResponse;
import com.quickseat.exception.BadRequestException;
import com.quickseat.repository.DashboardAnalyticsRepository;
import com.quickseat.repository.DashboardAnalyticsRepository.Filter;
import com.quickseat.repository.DashboardAnalyticsRepository.PerformanceRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final ZoneId MYANMAR_ZONE = ZoneId.of("Asia/Yangon");
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final DashboardAnalyticsRepository analyticsRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(LocalDate from, LocalDate to, Long cinemaId, Long movieId) {
        Filter filter = createFilter(from, to, cinemaId, movieId);
        LocalDate today = LocalDate.now(clock.withZone(MYANMAR_ZONE));
        Instant todayStart = today.atStartOfDay(MYANMAR_ZONE).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(MYANMAR_ZONE).toInstant();
        var summary = analyticsRepository.getSummary(filter, todayStart, tomorrowStart);
        var seats = analyticsRepository.getSeatTotals(filter);
        return new DashboardSummaryResponse(summary.totalBookings(), summary.confirmedBookings(),
                summary.todayBookings(), summary.revenue(), percentage(seats.bookedSeatUnits(), seats.capacitySeatUnits()),
                percentage(summary.cancelledBookings(), summary.totalBookings()));
    }

    @Transactional(readOnly = true)
    public List<MoviePerformanceResponse> getMoviePerformance(LocalDate from, LocalDate to, Long cinemaId,
                                                               Long movieId, int limit) {
        Filter filter = createFilter(from, to, cinemaId, movieId);
        return analyticsRepository.getMoviePerformance(filter, limit).stream().map(this::toMovieResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MoviePerformanceResponse> getMostBookedMovies(LocalDate from, LocalDate to, Long cinemaId,
                                                               Long movieId, int limit) {
        return getMoviePerformance(from, to, cinemaId, movieId, limit);
    }

    @Transactional(readOnly = true)
    public List<CinemaPerformanceResponse> getCinemaPerformance(LocalDate from, LocalDate to, Long cinemaId,
                                                                 Long movieId) {
        Filter filter = createFilter(from, to, cinemaId, movieId);
        return analyticsRepository.getCinemaPerformance(filter).stream().map(this::toCinemaResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RevenueTrendResponse> getRevenueTrend(LocalDate from, LocalDate to, Long cinemaId,
                                                       Long movieId, boolean monthly) {
        Filter filter = createFilter(from, to, cinemaId, movieId);
        return analyticsRepository.getRevenueTrend(filter, monthly).stream()
                .map(row -> new RevenueTrendResponse(row.periodStart(), row.revenue(), row.confirmedBookings()))
                .toList();
    }

    private Filter createFilter(LocalDate from, LocalDate to, Long cinemaId, Long movieId) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from date must be on or before to date");
        }
        Instant fromTime = from == null ? null : from.atStartOfDay(MYANMAR_ZONE).toInstant();
        Instant toTimeExclusive = to == null ? null : to.plusDays(1).atStartOfDay(MYANMAR_ZONE).toInstant();
        return new Filter(fromTime, toTimeExclusive, cinemaId, movieId);
    }

    private MoviePerformanceResponse toMovieResponse(PerformanceRow row) {
        return new MoviePerformanceResponse(row.id(), row.name(), row.totalBookings(), row.confirmedBookings(),
                row.cancelledBookings(), row.revenue(), row.bookedSeatUnits(), row.capacitySeatUnits(),
                percentage(row.bookedSeatUnits(), row.capacitySeatUnits()));
    }

    private CinemaPerformanceResponse toCinemaResponse(PerformanceRow row) {
        return new CinemaPerformanceResponse(row.id(), row.name(), row.totalBookings(), row.confirmedBookings(),
                row.cancelledBookings(), row.revenue(), row.bookedSeatUnits(), row.capacitySeatUnits(),
                percentage(row.bookedSeatUnits(), row.capacitySeatUnits()));
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(numerator).multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
