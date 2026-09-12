package com.quickseat.controller.customer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.response.cinema.CinemaResponse;
import com.quickseat.service.cinema.CinemaDiscoveryService;
import com.quickseat.service.showtime.ShowtimeDiscoveryService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PublicDiscoveryControllerTest {
    private CinemaDiscoveryService cinemaService;
    private ShowtimeDiscoveryService showtimeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cinemaService = mock(CinemaDiscoveryService.class);
        showtimeService = mock(ShowtimeDiscoveryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new CinemaController(cinemaService),
                new ShowtimeController(showtimeService)).build();
    }

    @Test
    void cinemaListIsAvailableWithoutAuthenticationAndForwardsFilters() throws Exception {
        CinemaResponse cinema = new CinemaResponse(1L, "Downtown", "Main Road", "Yangon",
                null, null, true, null, null);
        when(cinemaService.list(any(), any(), eq(1), eq(5)))
                .thenReturn(new PageResponse<>(List.of(cinema), 1, 5, 1, 1, true));

        mockMvc.perform(get("/cinemas")
                        .param("search", "down")
                        .param("city", "Yangon")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Downtown"));

        verify(cinemaService).list("down", "Yangon", 1, 5);
    }

    @Test
    void cinemaDetailIsAvailableWithoutAuthentication() throws Exception {
        CinemaResponse cinema = new CinemaResponse(1L, "Downtown", "Main Road", "Yangon",
                null, null, true, null, null);
        when(cinemaService.get(1L)).thenReturn(cinema);

        mockMvc.perform(get("/cinemas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void showtimeListIsAvailableWithoutAuthenticationAndForwardsFilters() throws Exception {
        when(showtimeService.list(any(), any(), any(), eq(0), eq(20)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/showtimes")
                        .param("movieId", "30")
                        .param("cinemaId", "10")
                        .param("date", "2026-09-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());

        verify(showtimeService).list(30L, 10L, LocalDate.of(2026, 9, 12), 0, 20);
    }
}
