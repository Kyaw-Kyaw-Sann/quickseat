package com.quickseat.controller.admin;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.response.user.CustomerResponse;
import com.quickseat.entity.enums.AuthProvider;
import com.quickseat.service.user.AdminUserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminCustomerControllerTest {
    private AdminUserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(AdminUserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCustomerController(userService)).build();
    }

    @Test
    void listsCustomers() throws Exception {
        when(userService.listCustomers(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(customer(true)), 0, 20, 1, 1, true));

        mockMvc.perform(get("/admin/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].email").value("customer@example.com"));
    }

    @Test
    void returnsCustomerDetail() throws Exception {
        when(userService.getCustomer(2L)).thenReturn(customer(true));

        mockMvc.perform(get("/admin/customers/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void updatesCustomerActiveStatus() throws Exception {
        when(userService.setCustomerActive(2L, false)).thenReturn(customer(false));

        mockMvc.perform(patch("/admin/customers/2/active")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
        verify(userService).setCustomerActive(2L, false);
    }

    private CustomerResponse customer(boolean active) {
        return new CustomerResponse(2L, "Customer", "customer@example.com", null,
                AuthProvider.LOCAL, true, active, null, null);
    }
}
