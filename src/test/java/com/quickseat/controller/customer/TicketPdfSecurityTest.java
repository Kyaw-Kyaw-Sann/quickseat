package com.quickseat.controller.customer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.config.SecurityConfig;
import com.quickseat.dto.response.ticket.TicketPdfDocument;
import com.quickseat.security.GoogleOAuthSuccessHandler;
import com.quickseat.security.JwtAuthenticationFilter;
import com.quickseat.security.RestAccessDeniedHandler;
import com.quickseat.security.RestAuthenticationEntryPoint;
import com.quickseat.service.ticket.TicketService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TicketController.class)
@Import({SecurityAutoConfiguration.class, SecurityConfig.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class TicketPdfSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean TicketService ticketService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean GoogleOAuthSuccessHandler googleOAuthSuccessHandler;

    @TestConfiguration
    @EnableWebSecurity
    static class TestWebSecurityConfiguration { }

    @BeforeEach
    void letRequestsContinueThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void anonymousUserCannotDownloadPdf() throws Exception {
        mockMvc.perform(get("/customer/tickets/ticket-token/pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotDownloadCustomerPdf() throws Exception {
        mockMvc.perform(get("/customer/tickets/ticket-token/pdf")
                        .with(user("staff@example.com").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanDownloadPdf() throws Exception {
        byte[] pdf = "%PDF-1.7 test".getBytes();
        when(ticketService.getPdf("ticket-token"))
                .thenReturn(new TicketPdfDocument("quickseat-ticket-QS-TICKET.pdf", pdf));

        mockMvc.perform(get("/customer/tickets/ticket-token/pdf")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));
    }
}
