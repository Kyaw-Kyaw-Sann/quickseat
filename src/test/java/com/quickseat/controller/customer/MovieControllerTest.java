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
import com.quickseat.entity.enums.MovieStatus;
import com.quickseat.service.movie.MovieService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MovieControllerTest {
    private MovieService movieService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        movieService = mock(MovieService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MovieController(movieService)).build();
    }

    @Test
    void publicMovieListAcceptsSearchFiltersAndPagination() throws Exception {
        when(movieService.listPublic(any(), any(), any(), eq(1), eq(5)))
                .thenReturn(new PageResponse<>(List.of(), 1, 5, 0, 0, true));

        mockMvc.perform(get("/movies")
                        .param("search", "inception")
                        .param("status", "NOW_SHOWING")
                        .param("language", "English")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());

        verify(movieService).listPublic("inception", MovieStatus.NOW_SHOWING, "English", 1, 5);
    }

    @Test
    void nowShowingAndUpcomingUseCorrectStatuses() throws Exception {
        when(movieService.listPublic(any(), any(), any(), eq(0), eq(20)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/movies/now-showing")).andExpect(status().isOk());
        mockMvc.perform(get("/movies/upcoming")).andExpect(status().isOk());

        verify(movieService).listPublic(null, MovieStatus.NOW_SHOWING, null, 0, 20);
        verify(movieService).listPublic(null, MovieStatus.UPCOMING, null, 0, 20);
    }
}
