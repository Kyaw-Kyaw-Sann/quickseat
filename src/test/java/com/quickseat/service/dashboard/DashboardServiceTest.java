package com.quickseat.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickseat.exception.BadRequestException;
import com.quickseat.repository.DashboardAnalyticsRepository;
import com.quickseat.repository.DashboardAnalyticsRepository.Filter;
import com.quickseat.repository.DashboardAnalyticsRepository.PerformanceRow;
import com.quickseat.repository.DashboardAnalyticsRepository.SeatTotalsRow;
import com.quickseat.repository.DashboardAnalyticsRepository.SummaryRow;
import com.quickseat.repository.DashboardAnalyticsRepository.TrendRow;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-10T12:00:00Z");

    @Mock DashboardAnalyticsRepository repository;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void calculatesSummaryRatesAndKeepsOnlyRepositoryRevenue() {
        when(repository.getSummary(any(), any(), any()))
                .thenReturn(new SummaryRow(10, 6, 2, new BigDecimal("45000.00"), 2));
        when(repository.getSeatTotals(any())).thenReturn(new SeatTotalsRow(5, 20));

        var response = service.getSummary(null, null, null, null);

        assertThat(response.totalRevenue()).isEqualByComparingTo("45000.00");
        assertThat(response.occupancyRate()).isEqualByComparingTo("25.00");
        assertThat(response.cancellationRate()).isEqualByComparingTo("20.00");
        assertThat(response.confirmedBookings()).isEqualTo(6);
    }

    @Test
    void returnsZeroRatesForEmptyScope() {
        when(repository.getSummary(any(), any(), any()))
                .thenReturn(new SummaryRow(0, 0, 0, BigDecimal.ZERO, 0));
        when(repository.getSeatTotals(any())).thenReturn(new SeatTotalsRow(0, 0));

        var response = service.getSummary(null, null, null, null);

        assertThat(response.occupancyRate()).isEqualByComparingTo("0.00");
        assertThat(response.cancellationRate()).isEqualByComparingTo("0.00");
    }

    @Test
    void convertsInclusiveMyanmarDateRangeAndFiltersConsistently() {
        when(repository.getMoviePerformance(any(), eq(10))).thenReturn(List.of());

        service.getMoviePerformance(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), 3L, 4L, 10);

        ArgumentCaptor<Filter> filter = ArgumentCaptor.forClass(Filter.class);
        verify(repository).getMoviePerformance(filter.capture(), eq(10));
        assertThat(filter.getValue().fromTime()).isEqualTo(Instant.parse("2026-08-31T17:30:00Z"));
        assertThat(filter.getValue().toTimeExclusive()).isEqualTo(Instant.parse("2026-09-10T17:30:00Z"));
        assertThat(filter.getValue().cinemaId()).isEqualTo(3L);
        assertThat(filter.getValue().movieId()).isEqualTo(4L);
    }

    @Test
    void rejectsReversedDateRange() {
        assertThatThrownBy(() -> service.getSummary(LocalDate.of(2026, 9, 11),
                LocalDate.of(2026, 9, 10), null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("from date must be on or before to date");
    }

    @Test
    void mapsMovieAndCinemaPerformanceWithOccupancy() {
        PerformanceRow row = new PerformanceRow(7, "Name", 8, 5, 2,
                new BigDecimal("30000.00"), 3, 12);
        when(repository.getMoviePerformance(any(), eq(20))).thenReturn(List.of(row));
        when(repository.getCinemaPerformance(any())).thenReturn(List.of(row));

        var movie = service.getMoviePerformance(null, null, null, null, 20).getFirst();
        var cinema = service.getCinemaPerformance(null, null, null, null).getFirst();

        assertThat(movie.movieTitle()).isEqualTo("Name");
        assertThat(movie.occupancyRate()).isEqualByComparingTo("25.00");
        assertThat(cinema.cinemaName()).isEqualTo("Name");
        assertThat(cinema.revenue()).isEqualByComparingTo("30000.00");
    }

    @Test
    void requestsDailyAndMonthlyRevenueTrends() {
        TrendRow row = new TrendRow(LocalDate.of(2026, 9, 1), new BigDecimal("10000.00"), 2);
        when(repository.getRevenueTrend(any(), eq(false))).thenReturn(List.of(row));
        when(repository.getRevenueTrend(any(), eq(true))).thenReturn(List.of(row));

        assertThat(service.getRevenueTrend(null, null, null, null, false)).hasSize(1);
        assertThat(service.getRevenueTrend(null, null, null, null, true)).hasSize(1);
        verify(repository).getRevenueTrend(any(), eq(false));
        verify(repository).getRevenueTrend(any(), eq(true));
    }
}
