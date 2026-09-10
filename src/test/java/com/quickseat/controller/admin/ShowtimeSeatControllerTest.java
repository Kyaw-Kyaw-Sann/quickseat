package com.quickseat.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.controller.customer.ShowtimeSeatController;
import com.quickseat.dto.response.showtime.ShowtimeSeatMapResponse;
import com.quickseat.dto.response.showtime.ShowtimeSeatResponse;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.service.showtime.ShowtimeSeatInventoryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ShowtimeSeatControllerTest {
    private ShowtimeSeatInventoryService inventoryService;
    private MockMvc adminMockMvc;
    private MockMvc publicMockMvc;

    @BeforeEach
    void setUp() {
        inventoryService = mock(ShowtimeSeatInventoryService.class);
        adminMockMvc = MockMvcBuilders.standaloneSetup(new AdminShowtimeSeatController(inventoryService)).build();
        publicMockMvc = MockMvcBuilders.standaloneSetup(new ShowtimeSeatController(inventoryService)).build();
    }

    @Test
    void adminGenerateEndpointReturnsCreatedSeatMap() throws Exception {
        when(inventoryService.generate(4L)).thenReturn(seatMap());

        adminMockMvc.perform(post("/admin/showtimes/4/seats/generate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.showtimeId").value(4))
                .andExpect(jsonPath("$.data.seats[0].price").value(5000.00));

        verify(inventoryService).generate(4L);
    }

    @Test
    void publicSeatMapEndpointReturnsOrderedSeats() throws Exception {
        when(inventoryService.getSeatMap(4L)).thenReturn(seatMap());

        publicMockMvc.perform(get("/showtimes/4/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.seats[0].rowName").value("A"))
                .andExpect(jsonPath("$.data.seats[0].seatNumber").value(1));

        verify(inventoryService).getSeatMap(4L);
    }

    private ShowtimeSeatMapResponse seatMap() {
        ShowtimeSeatResponse seat = new ShowtimeSeatResponse(100L, 10L, "A", 1, SeatType.NORMAL,
                new BigDecimal("5000.00"), SeatInventoryStatus.AVAILABLE);
        return new ShowtimeSeatMapResponse(4L, "Inventory Movie", "Screen 1",
                Instant.parse("2026-09-20T10:00:00Z"), List.of(seat));
    }
}
