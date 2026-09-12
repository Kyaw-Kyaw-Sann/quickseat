package com.quickseat.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.request.user.StaffCreateRequest;
import com.quickseat.dto.response.user.StaffResponse;
import com.quickseat.entity.enums.AuthProvider;
import com.quickseat.service.user.AdminUserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminStaffControllerTest {
    private AdminUserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(AdminUserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminStaffController(userService)).build();
    }

    @Test
    void createsStaffAccount() throws Exception {
        when(userService.createStaff(any(StaffCreateRequest.class))).thenReturn(staff(1L));

        mockMvc.perform(post("/admin/staff").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Staff","email":"staff@example.com","password":"password123","cinemaId":1}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cinemaId").value(1));
    }

    @Test
    void rejectsStaffWithoutCinema() throws Exception {
        mockMvc.perform(post("/admin/staff").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Staff","email":"staff@example.com","password":"password123"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsStaffAccounts() throws Exception {
        when(userService.listStaff(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(staff(1L)), 0, 20, 1, 1, true));

        mockMvc.perform(get("/admin/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].email").value("staff@example.com"));
    }

    @Test
    void assignsCinema() throws Exception {
        when(userService.assignCinema(3L, 9L)).thenReturn(staff(9L));

        mockMvc.perform(patch("/admin/staff/3/cinema")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cinemaId\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cinemaId").value(9));
        verify(userService).assignCinema(3L, 9L);
    }

    private StaffResponse staff(Long cinemaId) {
        return new StaffResponse(3L, "Staff", "staff@example.com", null, AuthProvider.LOCAL,
                true, cinemaId, "Cinema", null, null);
    }
}
