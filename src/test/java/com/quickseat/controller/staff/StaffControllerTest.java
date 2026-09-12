package com.quickseat.controller.staff;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.response.cinema.CinemaResponse;
import com.quickseat.dto.response.showtime.ShowtimeResponse;
import com.quickseat.dto.response.showtime.ShowtimeSeatMapResponse;
import com.quickseat.dto.response.staff.StaffBookingResponse;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.service.staff.StaffOperationsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StaffControllerTest {
    private StaffOperationsService staffService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        staffService = mock(StaffOperationsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StaffController(staffService)).build();
    }

    @Test
    void returnsAssignedCinema() throws Exception {
        when(staffService.getAssignedCinema()).thenReturn(
                new CinemaResponse(1L, "Cinema One", "Address", "Yangon", null,
                        null, true, null, null));

        mockMvc.perform(get("/staff/cinema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Cinema One"));
    }

    @Test
    void listsShowtimesWithPagination() throws Exception {
        when(staffService.listShowtimes(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(showtime()), 0, 20, 1, 1, true));

        mockMvc.perform(get("/staff/showtimes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].movieTitle").value("Staff Movie"));
    }

    @Test
    void returnsShowtimeSeatStatus() throws Exception {
        when(staffService.getShowtimeSeats(5L)).thenReturn(
                new ShowtimeSeatMapResponse(5L, "Staff Movie", "Screen 1",
                        Instant.parse("2026-09-11T10:00:00Z"), List.of()));

        mockMvc.perform(get("/staff/showtimes/5/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.showtimeId").value(5));
    }

    @Test
    void listsBookingsAndSupportsSearch() throws Exception {
        when(staffService.listBookings(isNull(), isNull(), isNull(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(booking()), 0, 20, 1, 1, true));

        mockMvc.perform(get("/staff/bookings").param("search", "QS-STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].bookingReference").value("QS-STAFF"));
    }

    private ShowtimeResponse showtime() {
        return new ShowtimeResponse(5L, 4L, "Staff Movie", 3L, "Screen 1", 1L, "Cinema One",
                Instant.parse("2026-09-11T10:00:00Z"), Instant.parse("2026-09-11T12:15:00Z"),
                15, new BigDecimal("5000.00"), new BigDecimal("9000.00"), ShowtimeStatus.ACTIVE,
                null, null);
    }

    private StaffBookingResponse booking() {
        return new StaffBookingResponse(7L, "QS-STAFF", BookingStatus.CONFIRMED,
                new BigDecimal("5000.00"), 6L, "Customer", "customer@example.com", 5L,
                "Staff Movie", 1L, "Cinema One", "Screen 1",
                Instant.parse("2026-09-11T10:00:00Z"), Instant.parse("2026-09-11T12:15:00Z"),
                List.of(), null, null, null);
    }
}
