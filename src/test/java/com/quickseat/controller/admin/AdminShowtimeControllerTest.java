package com.quickseat.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.service.showtime.ShowtimeService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminShowtimeControllerTest {
    private ShowtimeService showtimeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        showtimeService = mock(ShowtimeService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminShowtimeController(showtimeService)).build();
    }

    @Test
    void forwardsMovieCinemaDateStatusAndPaginationFilters() throws Exception {
        when(showtimeService.list(any(), any(), any(), any(), eq(1), eq(5)))
                .thenReturn(new PageResponse<>(List.of(), 1, 5, 0, 0, true));

        mockMvc.perform(get("/admin/showtimes")
                        .param("movieId", "30")
                        .param("cinemaId", "10")
                        .param("date", "2026-09-10")
                        .param("status", "ACTIVE")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());

        verify(showtimeService).list(30L, 10L, LocalDate.of(2026, 9, 10), ShowtimeStatus.ACTIVE, 1, 5);
    }
}
