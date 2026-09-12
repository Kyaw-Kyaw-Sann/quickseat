package com.quickseat.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DashboardAnalyticsRepositoryTest {

    @Test
    void summaryQueryCountsOnlySuccessfulRevenueForConfirmedOrUsedBookings() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        when(entityManager.createNativeQuery(sql.capture())).thenReturn(query);
        when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(new Object[]{10L, 6L, 2L, new BigDecimal("5000.00"), 1L});
        DashboardAnalyticsRepository repository = new DashboardAnalyticsRepository(entityManager);

        repository.getSummary(new DashboardAnalyticsRepository.Filter(null, null, null, null),
                Instant.parse("2026-09-10T00:00:00Z"), Instant.parse("2026-09-11T00:00:00Z"));

        assertThat(sql.getValue()).contains("p.status = 'SUCCESS'")
                .contains("b.status IN ('CONFIRMED', 'USED')")
                .contains("b.status = 'CANCELLED'");
    }

    @Test
    void occupancyQueryExcludesUnavailableAndCancelledShowtimes() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        when(entityManager.createNativeQuery(sql.capture())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(new Object[]{3L, 10L});
        DashboardAnalyticsRepository repository = new DashboardAnalyticsRepository(entityManager);

        var result = repository.getSeatTotals(new DashboardAnalyticsRepository.Filter(null, null, null, null));

        assertThat(result.bookedSeatUnits()).isEqualTo(3);
        assertThat(result.capacitySeatUnits()).isEqualTo(10);
        assertThat(sql.getValue()).contains("b.status IN ('CONFIRMED', 'USED')")
                .contains("ss.status <> 'UNAVAILABLE'")
                .contains("sh.status <> 'CANCELLED'");
    }
}
