package com.quickseat.service.staff;

import com.quickseat.dto.response.booking.BookingSeatResponse;
import com.quickseat.dto.response.staff.StaffTicketResponse;
import com.quickseat.dto.response.staff.TicketValidationResponse;
import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.Ticket;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.TicketStatus;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ForbiddenException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.TicketRepository;
import com.quickseat.security.CurrentUserService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketValidationService {
    private final TicketRepository ticketRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public StaffTicketResponse getTicket(String ticketToken) {
        User staff = currentUserService.getActiveStaffWithCinema();
        Ticket ticket = ticketRepository.findByTicketTokenWithDetails(ticketToken)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        verifyCinemaAccess(staff, ticket);
        return toResponse(ticket, bookingSeatRepository.findByBookingIdWithSeats(ticket.getBooking().getId()));
    }

    @Transactional
    public TicketValidationResponse validate(String ticketToken) {
        User staff = currentUserService.getActiveStaffWithCinema();
        Ticket ticket = ticketRepository.findByTicketTokenForUpdate(ticketToken)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        verifyCinemaAccess(staff, ticket);
        validateState(ticket);

        Instant now = clock.instant();
        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(now);
        ticket.setVerifiedBy(staff);
        ticket.getBooking().setStatus(BookingStatus.USED);
        ticketRepository.flush();
        List<BookingSeat> seats = bookingSeatRepository.findByBookingIdWithSeats(ticket.getBooking().getId());
        log.info("Ticket {} validated by staff {}", ticket.getId(), staff.getId());
        return new TicketValidationResponse("VALID", toResponse(ticket, seats));
    }

    private void verifyCinemaAccess(User staff, Ticket ticket) {
        Long ticketCinemaId = ticket.getBooking().getShowtime().getScreen().getCinema().getId();
        if (!staff.getCinema().getId().equals(ticketCinemaId)) {
            log.warn("Staff {} attempted cross-cinema ticket access", staff.getId());
            throw new ForbiddenException("Ticket belongs to another cinema");
        }
    }

    private void validateState(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.USED) {
            throw new ConflictException("Ticket has already been used");
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ConflictException("Ticket has been cancelled");
        }
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new ConflictException("Ticket status is invalid");
        }
        if (ticket.getBooking().getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("Ticket booking is not confirmed");
        }
    }

    private StaffTicketResponse toResponse(Ticket ticket, List<BookingSeat> seats) {
        Booking booking = ticket.getBooking();
        List<BookingSeatResponse> seatResponses = seats.stream()
                .map(item -> new BookingSeatResponse(item.getSeat().getId(), item.getSeat().getRowName(),
                        item.getSeat().getSeatNumber(), item.getSeatType(), item.getUnitPrice()))
                .toList();
        return new StaffTicketResponse(ticket.getId(), ticket.getTicketToken(), ticket.getStatus(),
                booking.getBookingReference(), booking.getStatus(), booking.getTotalAmount(), booking.getUser().getId(),
                booking.getUser().getName(), booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getScreen().getCinema().getName(), booking.getShowtime().getScreen().getName(),
                booking.getShowtime().getStartTime(), seatResponses, ticket.getUsedAt(),
                ticket.getVerifiedBy() == null ? null : ticket.getVerifiedBy().getId());
    }
}
