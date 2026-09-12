package com.quickseat.service.payment;

import com.quickseat.dto.request.payment.MockPaymentRequest;
import com.quickseat.dto.response.payment.PaymentResponse;
import com.quickseat.dto.response.payment.PaymentSummaryResponse;
import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.Payment;
import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.PaymentStatus;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.exception.BadRequestException;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.BookingRepository;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.PaymentRepository;
import com.quickseat.repository.ShowtimeSeatRepository;
import com.quickseat.security.CurrentUserService;
import com.quickseat.service.booking.SeatHoldService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MockPaymentService {
    private static final int REFERENCE_ATTEMPTS = 5;

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final CurrentUserService currentUserService;
    private final SeatHoldService seatHoldService;
    private final Clock clock;

    public MockPaymentService(BookingRepository bookingRepository,
                              BookingSeatRepository bookingSeatRepository,
                              PaymentRepository paymentRepository,
                              ShowtimeSeatRepository showtimeSeatRepository,
                              CurrentUserService currentUserService,
                              SeatHoldService seatHoldService,
                              Clock clock) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.paymentRepository = paymentRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.currentUserService = currentUserService;
        this.seatHoldService = seatHoldService;
        this.clock = clock;
    }

    @Transactional
    public PaymentSummaryResponse getSummary(String bookingReference) {
        User customer = currentUserService.getVerifiedCustomer();
        Booking booking = getCustomerBooking(bookingReference, customer.getId());
        Instant now = clock.instant();
        seatHoldService.expirePendingBookingIfNeeded(booking, now);
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        return toSummary(booking, payment, now);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public PaymentResponse process(String bookingReference, MockPaymentRequest request) {
        User customer = currentUserService.getVerifiedCustomer();
        Booking booking = getCustomerBooking(bookingReference, customer.getId());
        Instant now = clock.instant();
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        BigDecimal requestedAmount = request.amount() == null ? booking.getTotalAmount() : request.amount();
        validateAmount(requestedAmount, booking.getTotalAmount());

        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            return handleRepeatedSuccess(booking, payment, request, requestedAmount);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException("Only a pending booking can be paid");
        }
        if (booking.getExpiresAt() == null || !booking.getExpiresAt().isAfter(now)) {
            seatHoldService.expirePendingBooking(booking);
            throw new BadRequestException("Booking has expired and cannot be paid");
        }
        if (!booking.getShowtime().getStartTime().isAfter(now)) {
            seatHoldService.expirePendingBooking(booking);
            throw new BadRequestException("Payment is not allowed after the showtime starts");
        }

        List<BookingSeat> snapshots = bookingSeatRepository.findByBookingIdWithSeats(booking.getId());
        List<ShowtimeSeat> heldSeats = showtimeSeatRepository
                .findHeldSeatsByBookingIdForUpdate(booking.getId());
        validateHeldSeats(booking, snapshots, heldSeats, now);

        Payment currentPayment = payment == null
                ? createPayment(booking, requestedAmount)
                : prepareFailedPaymentRetry(payment, booking.getTotalAmount());
        if (!request.successful()) {
            currentPayment.setStatus(PaymentStatus.FAILED);
            currentPayment.setPaidAt(null);
            Payment saved = paymentRepository.save(currentPayment);
            log.info("Mock payment failed for booking {}", booking.getId());
            return toResponse(saved, booking, false);
        }

        currentPayment.setStatus(PaymentStatus.SUCCESS);
        currentPayment.setPaidAt(now);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(now);
        heldSeats.forEach(showtimeSeat -> {
            showtimeSeat.setStatus(SeatInventoryStatus.BOOKED);
            showtimeSeat.setHeldByBooking(null);
            showtimeSeat.setHoldExpiresAt(null);
        });
        Payment saved = paymentRepository.save(currentPayment);
        paymentRepository.flush();
        log.info("Mock payment succeeded for booking {}", booking.getId());
        return toResponse(saved, booking, false);
    }

    private PaymentResponse handleRepeatedSuccess(Booking booking, Payment payment,
                                                   MockPaymentRequest request, BigDecimal requestedAmount) {
        if (!request.successful()) {
            throw new ConflictException("A successful payment cannot be changed to failed");
        }
        validateAmount(payment.getAmount(), booking.getTotalAmount());
        validateAmount(requestedAmount, payment.getAmount());
        if (booking.getStatus() != BookingStatus.CONFIRMED || payment.getPaidAt() == null) {
            throw new ConflictException("Successful payment state is inconsistent with the booking");
        }

        List<BookingSeat> snapshots = bookingSeatRepository.findByBookingIdWithSeats(booking.getId());
        List<Long> seatIds = snapshots.stream().map(snapshot -> snapshot.getSeat().getId()).toList();
        List<ShowtimeSeat> inventory = seatIds.isEmpty() ? List.of()
                : showtimeSeatRepository.findBookingSeatsForUpdate(booking.getShowtime().getId(), seatIds);
        if (inventory.size() != snapshots.size()
                || inventory.stream().anyMatch(seat -> seat.getStatus() != SeatInventoryStatus.BOOKED
                        || seat.getHeldByBooking() != null || seat.getHoldExpiresAt() != null)) {
            throw new ConflictException("Successful payment seat state is inconsistent");
        }
        return toResponse(payment, booking, true);
    }

    private void validateHeldSeats(Booking booking, List<BookingSeat> snapshots,
                                   List<ShowtimeSeat> heldSeats, Instant now) {
        if (snapshots.isEmpty() || heldSeats.size() != snapshots.size()) {
            throw new ConflictException("Held seats are incomplete for this booking");
        }
        Set<Long> snapshotSeatIds = new HashSet<>();
        BigDecimal snapshotTotal = BigDecimal.ZERO;
        for (BookingSeat snapshot : snapshots) {
            snapshotSeatIds.add(snapshot.getSeat().getId());
            snapshotTotal = snapshotTotal.add(snapshot.getUnitPrice());
        }
        validateAmount(snapshotTotal, booking.getTotalAmount());

        for (ShowtimeSeat heldSeat : heldSeats) {
            if (heldSeat.getStatus() != SeatInventoryStatus.HELD
                    || heldSeat.getHeldByBooking() == null
                    || !booking.getId().equals(heldSeat.getHeldByBooking().getId())
                    || heldSeat.getHoldExpiresAt() == null
                    || !heldSeat.getHoldExpiresAt().isAfter(now)
                    || !heldSeat.getSeat().isActive()
                    || !snapshotSeatIds.remove(heldSeat.getSeat().getId())) {
                throw new ConflictException("Seat hold state is inconsistent for this booking");
            }
        }
        if (!snapshotSeatIds.isEmpty()) {
            throw new ConflictException("Held seats do not match the booking seats");
        }
    }

    private Payment prepareFailedPaymentRetry(Payment payment, BigDecimal bookingTotal) {
        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new ConflictException("Existing payment cannot be retried");
        }
        validateAmount(payment.getAmount(), bookingTotal);
        return payment;
    }

    private Payment createPayment(Booking booking, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentReference(generatePaymentReference());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(amount);
        return payment;
    }

    private Booking getCustomerBooking(String bookingReference, Long customerId) {
        return bookingRepository.findCustomerBookingForUpdate(bookingReference, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private void validateAmount(BigDecimal actual, BigDecimal expected) {
        if (actual == null || expected == null || actual.compareTo(expected) != 0) {
            throw new BadRequestException("Payment amount does not match the booking total");
        }
    }

    private PaymentSummaryResponse toSummary(Booking booking, Payment payment, Instant now) {
        long remainingSeconds = booking.getStatus() == BookingStatus.PENDING && booking.getExpiresAt() != null
                ? Math.max(0, Duration.between(now, booking.getExpiresAt()).getSeconds())
                : 0;
        boolean canPay = booking.getStatus() == BookingStatus.PENDING
                && booking.getExpiresAt() != null
                && booking.getExpiresAt().isAfter(now)
                && booking.getShowtime().getStartTime().isAfter(now);
        return new PaymentSummaryResponse(booking.getBookingReference(), booking.getStatus(),
                booking.getTotalAmount(), booking.getExpiresAt(), remainingSeconds,
                payment == null ? null : payment.getPaymentReference(),
                payment == null ? null : payment.getStatus(), canPay);
    }

    private PaymentResponse toResponse(Payment payment, Booking booking, boolean alreadyProcessed) {
        return new PaymentResponse(payment.getId(), payment.getPaymentReference(), payment.getStatus(),
                payment.getAmount(), payment.getPaidAt(), booking.getBookingReference(), booking.getStatus(),
                booking.getConfirmedAt(), alreadyProcessed);
    }

    private String generatePaymentReference() {
        for (int attempt = 0; attempt < REFERENCE_ATTEMPTS; attempt++) {
            String reference = "PAY-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 16).toUpperCase();
            if (!paymentRepository.existsByPaymentReference(reference)) {
                return reference;
            }
        }
        throw new ConflictException("Could not generate a unique payment reference");
    }
}
