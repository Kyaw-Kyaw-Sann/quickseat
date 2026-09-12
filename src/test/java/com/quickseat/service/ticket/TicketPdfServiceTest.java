package com.quickseat.service.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.Movie;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Seat;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.Ticket;
import com.quickseat.entity.enums.SeatType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class TicketPdfServiceTest {

    @Test
    void createsReadablePdfWithTicketDetailsAndValidQrImage() throws Exception {
        String qrContent = "https://api.quickseat.test/api/v1/staff/tickets/validate?token=ticket-token";
        byte[] qrImage = new com.quickseat.service.shared.QrCodeService(320).generatePng(qrContent);

        byte[] pdf = new TicketPdfService().generate(ticket(), List.of(bookingSeat()), qrImage);

        assertThat(pdf).startsWith("%PDF".getBytes());
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("QuickSeat", "QS-TICKET", "Test Movie", "A5 (NORMAL)", "8000.00 MMK");

            PDImageXObject embeddedQr = null;
            for (COSName name : document.getPage(0).getResources().getXObjectNames()) {
                PDXObject object = document.getPage(0).getResources().getXObject(name);
                if (object instanceof PDImageXObject image) {
                    embeddedQr = image;
                    break;
                }
            }
            assertThat(embeddedQr).isNotNull();
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                    new BufferedImageLuminanceSource(embeddedQr.getImage())));
            assertThat(new MultiFormatReader().decode(bitmap).getText()).isEqualTo(qrContent);
        }
    }

    private Ticket ticket() {
        Cinema cinema = new Cinema();
        cinema.setName("QuickSeat Cinema");
        Screen screen = new Screen();
        screen.setName("Screen 1");
        screen.setCinema(cinema);
        Movie movie = new Movie();
        movie.setTitle("Test Movie");
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(Instant.parse("2026-09-11T10:00:00Z"));

        Booking booking = new Booking();
        booking.setId(20L);
        booking.setBookingReference("QS-TICKET");
        booking.setShowtime(showtime);
        booking.setTotalAmount(new BigDecimal("8000.00"));

        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setTicketToken("ticket-token");
        return ticket;
    }

    private BookingSeat bookingSeat() {
        Seat seat = new Seat();
        seat.setRowName("A");
        seat.setSeatNumber(5);
        BookingSeat bookingSeat = new BookingSeat();
        bookingSeat.setSeat(seat);
        bookingSeat.setSeatType(SeatType.NORMAL);
        bookingSeat.setUnitPrice(new BigDecimal("8000.00"));
        return bookingSeat;
    }
}
