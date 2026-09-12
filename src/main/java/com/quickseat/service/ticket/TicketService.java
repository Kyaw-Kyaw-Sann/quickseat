package com.quickseat.service.ticket;

import com.quickseat.dto.response.booking.BookingSeatResponse;
import com.quickseat.dto.response.ticket.TicketPdfDocument;
import com.quickseat.dto.response.ticket.TicketResponse;
import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.Payment;
import com.quickseat.entity.Ticket;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.PaymentStatus;
import com.quickseat.entity.enums.TicketStatus;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.BookingRepository;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.PaymentRepository;
import com.quickseat.repository.TicketRepository;
import com.quickseat.security.CurrentUserService;
import com.quickseat.service.shared.EmailService;
import com.quickseat.service.shared.QrCodeService;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
public class TicketService {
    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_ATTEMPTS = 5;

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;
    private final QrCodeService qrCodeService;
    private final TicketPdfService ticketPdfService;
    private final EmailService emailService;
    private final SecureRandom secureRandom;
    private final String validationBaseUrl;

    @Autowired
    public TicketService(BookingRepository bookingRepository,
                         BookingSeatRepository bookingSeatRepository,
                         PaymentRepository paymentRepository,
                         TicketRepository ticketRepository,
                         CurrentUserService currentUserService,
                         QrCodeService qrCodeService,
                         TicketPdfService ticketPdfService,
                         EmailService emailService,
                         @Value("${app.ticket.validation-base-url}") String validationBaseUrl) {
        this(bookingRepository, bookingSeatRepository, paymentRepository, ticketRepository,
                currentUserService, qrCodeService, ticketPdfService, emailService,
                new SecureRandom(), validationBaseUrl);
    }

    TicketService(BookingRepository bookingRepository,
                  BookingSeatRepository bookingSeatRepository,
                  PaymentRepository paymentRepository,
                  TicketRepository ticketRepository,
                  CurrentUserService currentUserService,
                  QrCodeService qrCodeService,
                  TicketPdfService ticketPdfService,
                  EmailService emailService,
                  SecureRandom secureRandom,
                  String validationBaseUrl) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
        this.currentUserService = currentUserService;
        this.qrCodeService = qrCodeService;
        this.ticketPdfService = ticketPdfService;
        this.emailService = emailService;
        this.secureRandom = secureRandom;
        this.validationBaseUrl = validationBaseUrl;
    }

    @Transactional
    public TicketResponse generate(String bookingReference) {
        User customer = currentUserService.getVerifiedCustomer();
        Booking booking = getCustomerBooking(bookingReference, customer.getId());
        validatePaidBooking(booking);

        Ticket existing = ticketRepository.findByBookingId(booking.getId()).orElse(null);
        List<BookingSeat> seats = bookingSeatRepository.findByBookingIdWithSeats(booking.getId());
        if (existing != null) {
            if (existing.getStatus() != TicketStatus.ACTIVE) {
                throw new ConflictException("Existing ticket is not active");
            }
            return toResponse(existing, seats, true);
        }

        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setTicketToken(generateUniqueToken());
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setQrImageUrl("/api/v1/customer/tickets/" + ticket.getTicketToken() + "/qr");
        Ticket saved = ticketRepository.saveAndFlush(ticket);
        sendConfirmationAfterCommit(saved, seats, customer.getEmail());
        log.info("Ticket generated for booking {}", booking.getId());
        return toResponse(saved, seats, false);
    }

    @Transactional
    public TicketResponse getByBooking(String bookingReference) {
        User customer = currentUserService.getVerifiedCustomer();
        Booking booking = getCustomerBooking(bookingReference, customer.getId());
        Ticket ticket = ticketRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        List<BookingSeat> seats = bookingSeatRepository.findByBookingIdWithSeats(booking.getId());
        return toResponse(ticket, seats, false);
    }

    @Transactional(readOnly = true)
    public byte[] getQrImage(String ticketToken) {
        Ticket ticket = getOwnedTicket(ticketToken);
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new ConflictException("Only an active ticket has an available QR code");
        }
        return qrCodeService.generatePng(buildQrContent(ticket.getTicketToken()));
    }

    @Transactional(readOnly = true)
    public TicketPdfDocument getPdf(String ticketToken) {
        Ticket ticket = getOwnedTicket(ticketToken);
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new ConflictException("Only an active ticket can be downloaded as PDF");
        }
        List<BookingSeat> seats = bookingSeatRepository.findByBookingIdWithSeats(ticket.getBooking().getId());
        byte[] qrImage = qrCodeService.generatePng(buildQrContent(ticket.getTicketToken()));
        byte[] pdf = ticketPdfService.generate(ticket, seats, qrImage);
        String filename = "quickseat-ticket-" + ticket.getBooking().getBookingReference() + ".pdf";
        return new TicketPdfDocument(filename, pdf);
    }

    private Ticket getOwnedTicket(String ticketToken) {
        User customer = currentUserService.getVerifiedCustomer();
        return ticketRepository.findCustomerTicketByToken(ticketToken, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private void validatePaidBooking(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("Only a confirmed booking can generate a ticket");
        }
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ConflictException("Successful payment is required to generate a ticket"));
        if (payment.getStatus() != PaymentStatus.SUCCESS || payment.getPaidAt() == null
                || payment.getAmount() == null || booking.getTotalAmount() == null
                || payment.getAmount().compareTo(booking.getTotalAmount()) != 0) {
            throw new ConflictException("Successful payment state is inconsistent with the booking");
        }
    }

    private Booking getCustomerBooking(String bookingReference, Long customerId) {
        return bookingRepository.findCustomerBookingForUpdate(bookingReference, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private String generateUniqueToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        for (int attempt = 0; attempt < TOKEN_ATTEMPTS; attempt++) {
            secureRandom.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            if (!ticketRepository.existsByTicketToken(token)) {
                return token;
            }
        }
        throw new ConflictException("Could not generate a unique ticket token");
    }

    private String buildQrContent(String ticketToken) {
        String separator = validationBaseUrl.contains("?") ? "&" : "?";
        return validationBaseUrl + separator + "token=" + ticketToken;
    }

    private void sendConfirmationAfterCommit(Ticket ticket, List<BookingSeat> seats, String recipient) {
        Runnable emailTask = () -> sendConfirmationEmail(ticket, seats, recipient);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailTask.run();
                }
            });
        } else {
            emailTask.run();
        }
    }

    private void sendConfirmationEmail(Ticket ticket, List<BookingSeat> seats, String recipient) {
        try {
            byte[] qrImage = qrCodeService.generatePng(buildQrContent(ticket.getTicketToken()));
            emailService.sendWithAttachment(recipient, "QuickSeat booking confirmed",
                    buildEmailBody(ticket.getBooking(), seats), "quickseat-ticket.png", qrImage, "image/png");
        } catch (RuntimeException exception) {
            log.error("Booking confirmation email failed for booking {}", ticket.getBooking().getId(), exception);
        }
    }

    private String buildEmailBody(Booking booking, List<BookingSeat> seats) {
        String seatLabels = seats.stream()
                .map(seat -> seat.getSeat().getRowName() + seat.getSeat().getSeatNumber())
                .reduce((first, second) -> first + ", " + second)
                .orElse("-");
        return """
                Your QuickSeat booking is confirmed.

                Booking reference: %s
                Movie: %s
                Cinema: %s
                Screen: %s
                Showtime: %s
                Seats: %s
                Total: %s MMK

                Your QR ticket is attached to this email.
                """.formatted(booking.getBookingReference(), booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getScreen().getCinema().getName(), booking.getShowtime().getScreen().getName(),
                booking.getShowtime().getStartTime(), seatLabels, booking.getTotalAmount().toPlainString());
    }

    private TicketResponse toResponse(Ticket ticket, List<BookingSeat> seats, boolean alreadyGenerated) {
        Booking booking = ticket.getBooking();
        List<BookingSeatResponse> seatResponses = seats.stream()
                .map(seat -> new BookingSeatResponse(seat.getSeat().getId(), seat.getSeat().getRowName(),
                        seat.getSeat().getSeatNumber(), seat.getSeatType(), seat.getUnitPrice()))
                .toList();
        return new TicketResponse(ticket.getId(), ticket.getTicketToken(), ticket.getStatus(), ticket.getQrImageUrl(),
                booking.getBookingReference(), booking.getTotalAmount(), booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getScreen().getCinema().getName(), booking.getShowtime().getScreen().getName(),
                booking.getShowtime().getStartTime(), booking.getShowtime().getEndTime(), seatResponses,
                ticket.getCreatedAt(), alreadyGenerated);
    }
}
