package com.quickseat.controller.staff;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.request.staff.TicketValidationRequest;
import com.quickseat.dto.response.staff.StaffTicketResponse;
import com.quickseat.dto.response.staff.TicketValidationResponse;
import com.quickseat.service.staff.TicketValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/staff/tickets")
@RequiredArgsConstructor
public class StaffTicketController {
    private final TicketValidationService ticketService;

    @GetMapping("/{ticketToken}")
    public ApiResponse<StaffTicketResponse> getTicket(@PathVariable String ticketToken) {
        return ApiResponse.success("Ticket retrieved successfully", ticketService.getTicket(ticketToken));
    }

    @PostMapping("/validate")
    public ApiResponse<TicketValidationResponse> validate(@Valid @RequestBody TicketValidationRequest request) {
        return ApiResponse.success("Ticket validated successfully", ticketService.validate(request.ticketToken()));
    }
}
