package com.quickseat.controller.customer;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.response.ticket.TicketPdfDocument;
import com.quickseat.dto.response.ticket.TicketResponse;
import com.quickseat.service.ticket.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @PostMapping("/bookings/{bookingReference}/ticket")
    public ApiResponse<TicketResponse> generate(@PathVariable String bookingReference) {
        TicketResponse ticket = ticketService.generate(bookingReference);
        String message = ticket.alreadyGenerated()
                ? "Ticket already generated"
                : "Ticket generated successfully";
        return ApiResponse.success(message, ticket);
    }

    @GetMapping("/bookings/{bookingReference}/ticket")
    public ApiResponse<TicketResponse> getByBooking(@PathVariable String bookingReference) {
        return ApiResponse.success("Ticket retrieved successfully", ticketService.getByBooking(bookingReference));
    }

    @GetMapping(value = "/tickets/{ticketToken}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrImage(@PathVariable String ticketToken,
                                              @RequestParam(defaultValue = "false") boolean download) {
        ContentDisposition disposition = download
                ? ContentDisposition.attachment().filename("quickseat-ticket.png").build()
                : ContentDisposition.inline().filename("quickseat-ticket.png").build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.IMAGE_PNG)
                .body(ticketService.getQrImage(ticketToken));
    }

    @GetMapping(value = "/tickets/{ticketToken}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPdf(@PathVariable String ticketToken) {
        TicketPdfDocument document = ticketService.getPdf(ticketToken);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.filename())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(document.content());
    }
}
