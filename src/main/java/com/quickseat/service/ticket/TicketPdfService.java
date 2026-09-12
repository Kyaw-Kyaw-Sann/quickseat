package com.quickseat.service.ticket;

import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.Ticket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

@Service
public class TicketPdfService {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    public byte[] generate(Ticket ticket, List<BookingSeat> seats, byte[] qrImage) {
        Booking booking = ticket.getBooking();
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                writeText(content, bold, 25, 55, 790, "QuickSeat");
                writeText(content, bold, 15, 55, 765, "Movie Ticket");
                content.moveTo(55, 750);
                content.lineTo(540, 750);
                content.stroke();

                float y = 720;
                y = writeDetail(content, regular, bold, y, "Booking reference", booking.getBookingReference());
                y = writeDetail(content, regular, bold, y, "Movie", booking.getShowtime().getMovie().getTitle());
                y = writeDetail(content, regular, bold, y, "Cinema", booking.getShowtime().getScreen().getCinema().getName());
                y = writeDetail(content, regular, bold, y, "Screen", booking.getShowtime().getScreen().getName());
                y = writeDetail(content, regular, bold, y, "Showtime", TIME_FORMAT.format(booking.getShowtime().getStartTime()));
                y = writeDetail(content, regular, bold, y, "Seats", seatLabels(seats));
                y = writeDetail(content, regular, bold, y, "Total", booking.getTotalAmount().toPlainString() + " MMK");
                y = writeDetail(content, regular, bold, y, "Ticket token", ticket.getTicketToken());

                PDImageXObject qr = PDImageXObject.createFromByteArray(document, qrImage, "ticket-qr");
                float qrY = Math.max(120, Math.min(300, y - 205));
                content.drawImage(qr, 208, qrY, 180, 180);
                writeText(content, bold, 11, 196, qrY - 20, "Present this QR code at the cinema");
                writeText(content, regular, 9, 55, qrY - 50,
                        "This ticket is valid for one entry and can be used only once.");
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate the ticket PDF", exception);
        }
    }

    private float writeDetail(PDPageContentStream content, PDType1Font regular, PDType1Font bold,
                              float y, String label, String value) throws IOException {
        writeText(content, bold, 11, 55, y, label + ":");
        List<String> lines = wrapText(value, 58);
        float lineY = y;
        for (String line : lines) {
            writeText(content, regular, 11, 180, lineY, line);
            lineY -= 16;
        }
        return lineY - 12;
    }

    private void writeText(PDPageContentStream content, PDType1Font font, float size,
                           float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safeText(text));
        content.endText();
    }

    private String seatLabels(List<BookingSeat> seats) {
        return seats.stream()
                .map(seat -> seat.getSeat().getRowName() + seat.getSeat().getSeatNumber()
                        + " (" + seat.getSeatType() + ")")
                .collect(Collectors.joining(", "));
    }

    private List<String> wrapText(String value, int maxCharacters) {
        String remaining = safeText(value);
        List<String> lines = new ArrayList<>();
        while (remaining.length() > maxCharacters) {
            int breakAt = remaining.lastIndexOf(' ', maxCharacters);
            if (breakAt <= 0) {
                breakAt = maxCharacters;
            }
            lines.add(remaining.substring(0, breakAt).trim());
            remaining = remaining.substring(breakAt).trim();
        }
        lines.add(remaining);
        return lines;
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        StringBuilder result = new StringBuilder(value.length());
        for (char character : value.toCharArray()) {
            result.append(character >= 32 && character <= 126 ? character : '?');
        }
        return result.toString();
    }
}
