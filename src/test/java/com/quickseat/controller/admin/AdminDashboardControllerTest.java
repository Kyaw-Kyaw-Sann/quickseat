package com.quickseat.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.response.dashboard.CinemaPerformanceResponse;
import com.quickseat.dto.response.dashboard.DashboardSummaryResponse;
import com.quickseat.dto.response.dashboard.MoviePerformanceResponse;
import com.quickseat.dto.response.dashboard.RevenueTrendResponse;
import com.quickseat.service.dashboard.DashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminDashboardControllerTest {
    private DashboardService dashboardService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        dashboardService = mock(DashboardService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(dashboardService)).build();
    }

    @Test
    void returnsDashboardSummaryWithFilters() throws Exception {
        when(dashboardService.getSummary(any(), any(), eq(1L), eq(2L)))
                .thenReturn(new DashboardSummaryResponse(10, 6, 2, new BigDecimal("50000.00"),
                        new BigDecimal("30.00"), new BigDecimal("10.00")));

        mockMvc.perform(get("/admin/dashboard/summary")
                        .param("from", "2026-09-01").param("to", "2026-09-10")
                        .param("cinemaId", "1").param("movieId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBookings").value(10))
                .andExpect(jsonPath("$.data.totalRevenue").value(50000.00));
    }

    @Test
    void returnsMovieAndMostBookedPerformance() throws Exception {
        MoviePerformanceResponse movie = new MoviePerformanceResponse(2L, "Movie", 5, 4, 1,
                new BigDecimal("20000.00"), 4, 10, new BigDecimal("40.00"));
        when(dashboardService.getMoviePerformance(isNull(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(movie));
        when(dashboardService.getMostBookedMovies(isNull(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(movie));

        mockMvc.perform(get("/admin/dashboard/movies"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].movieTitle").value("Movie"));
        mockMvc.perform(get("/admin/dashboard/movies/top"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].confirmedBookings").value(4));
    }

    @Test
    void returnsCinemaPerformance() throws Exception {
        when(dashboardService.getCinemaPerformance(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(new CinemaPerformanceResponse(1L, "Cinema", 5, 4, 1,
                        new BigDecimal("20000.00"), 4, 10, new BigDecimal("40.00"))));

        mockMvc.perform(get("/admin/dashboard/cinemas"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].cinemaName").value("Cinema"));
    }

    @Test
    void returnsDailyAndMonthlyRevenueTrends() throws Exception {
        RevenueTrendResponse trend = new RevenueTrendResponse(LocalDate.of(2026, 9, 1),
                new BigDecimal("10000.00"), 2);
        when(dashboardService.getRevenueTrend(isNull(), isNull(), isNull(), isNull(), eq(false)))
                .thenReturn(List.of(trend));
        when(dashboardService.getRevenueTrend(isNull(), isNull(), isNull(), isNull(), eq(true)))
                .thenReturn(List.of(trend));

        mockMvc.perform(get("/admin/dashboard/revenue/daily"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].revenue").value(10000.00));
        mockMvc.perform(get("/admin/dashboard/revenue/monthly"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].periodStart").value("2026-09-01"));
    }
}
