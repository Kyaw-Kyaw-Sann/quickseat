package com.quickseat.controller.admin;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.response.booking.AdminBookingResponse;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.service.booking.AdminBookingService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminBookingControllerTest {
    private AdminBookingService bookingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        bookingService = mock(AdminBookingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminBookingController(bookingService)).build();
    }

    @Test
    void listsSystemBookings() throws Exception {
        when(bookingService.list(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(booking(BookingStatus.CONFIRMED)),
                        0, 20, 1, 1, true));

        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].bookingReference").value("QS-ADMIN"));
    }

    @Test
    void getsBookingDetail() throws Exception {
        when(bookingService.get("QS-ADMIN")).thenReturn(booking(BookingStatus.CONFIRMED));

        mockMvc.perform(get("/admin/bookings/QS-ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void cancelsEligibleBooking() throws Exception {
        when(bookingService.cancel("QS-ADMIN")).thenReturn(booking(BookingStatus.CANCELLED));

        mockMvc.perform(patch("/admin/bookings/QS-ADMIN/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    private AdminBookingResponse booking(BookingStatus status) {
        return new AdminBookingResponse(6L, "QS-ADMIN", status, new BigDecimal("5000.00"),
                5L, "Customer", "customer@example.com", 4L, 3L, "Movie", 1L, "Cinema",
                "Screen 1", null, null, List.of(), null, null, null, null);
    }
}
