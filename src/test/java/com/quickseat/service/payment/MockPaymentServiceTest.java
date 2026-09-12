package com.quickseat.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickseat.dto.request.payment.MockPaymentRequest;
import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.Movie;
import com.quickseat.entity.Payment;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Seat;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.PaymentStatus;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.entity.enums.SeatType;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@ExtendWith(MockitoExtension.class)
class MockPaymentServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    @Mock BookingRepository bookingRepository;
    @Mock BookingSeatRepository bookingSeatRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock ShowtimeSeatRepository showtimeSeatRepository;
    @Mock CurrentUserService currentUserService;
    @Mock SeatHoldService seatHoldService;

    private MockPaymentService service;
    private User customer;
    private Booking booking;
    private BookingSeat snapshot;
    private ShowtimeSeat heldSeat;

    @BeforeEach
    void setUp() {
        service = new MockPaymentService(bookingRepository, bookingSeatRepository, paymentRepository,
                showtimeSeatRepository, currentUserService, seatHoldService,
                Clock.fixed(NOW, ZoneOffset.UTC));

        customer = new User();
        customer.setId(50L);
        customer.setEmailVerified(true);

        Cinema cinema = new Cinema();
        cinema.setActive(true);
        Screen screen = new Screen();
        screen.setId(2L);
        screen.setActive(true);
        screen.setCinema(cinema);
        Movie movie = new Movie();
        movie.setActive(true);
        Showtime showtime = new Showtime();
        showtime.setId(4L);
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(NOW.plusSeconds(7_200));

        booking = new Booking();
        booking.setId(5L);
        booking.setBookingReference("QS-PAYMENT");
        booking.setUser(customer);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(new BigDecimal("5000.00"));
        booking.setExpiresAt(NOW.plusSeconds(300));

        Seat seat = new Seat();
        seat.setId(6L);
        seat.setSeatType(SeatType.NORMAL);
        seat.setActive(true);
        snapshot = new BookingSeat();
        snapshot.setBooking(booking);
        snapshot.setSeat(seat);
        snapshot.setSeatType(SeatType.NORMAL);
        snapshot.setUnitPrice(new BigDecimal("5000.00"));

        heldSeat = new ShowtimeSeat();
        heldSeat.setId(7L);
        heldSeat.setShowtime(showtime);
        heldSeat.setSeat(seat);
        heldSeat.setStatus(SeatInventoryStatus.HELD);
        heldSeat.setHeldByBooking(booking);
        heldSeat.setHoldExpiresAt(booking.getExpiresAt());
    }

    @Test
    void returnsPaymentSummaryForValidPendingBooking() {
        mockOwnedBooking();

        var result = service.getSummary("QS-PAYMENT");

        assertThat(result.bookingStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.totalAmount()).isEqualByComparingTo("5000.00");
        assertThat(result.remainingSeconds()).isEqualTo(300);
        assertThat(result.canPay()).isTrue();
        assertThat(result.paymentStatus()).isNull();
    }

    @Test
    void successfulPaymentConfirmsBookingAndBooksHeldSeats() {
        mockValidPayment(null);

        var result = service.process("QS-PAYMENT", new MockPaymentRequest(true, null));

        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.confirmedAt()).isEqualTo(NOW);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(heldSeat.getStatus()).isEqualTo(SeatInventoryStatus.BOOKED);
        assertThat(heldSeat.getHeldByBooking()).isNull();
        assertThat(heldSeat.getHoldExpiresAt()).isNull();
        verify(paymentRepository).flush();
    }

    @Test
    void failedPaymentKeepsBookingPending() {
        mockValidPayment(null);

        var result = service.process("QS-PAYMENT", new MockPaymentRequest(false, null));

        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(heldSeat.getStatus()).isEqualTo(SeatInventoryStatus.HELD);
        verify(paymentRepository, never()).flush();
    }

    @Test
    void failedPaymentCanBeRetriedBeforeExpiration() {
        Payment failedPayment = payment(PaymentStatus.FAILED);
        mockValidPayment(failedPayment);

        var result = service.process("QS-PAYMENT", new MockPaymentRequest(true,
                new BigDecimal("5000.00")));

        assertThat(result.paymentId()).isEqualTo(8L);
        assertThat(result.paymentReference()).isEqualTo("PAY-TEST");
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void expiredPendingBookingCannotBePaidAndIsExpired() {
        booking.setExpiresAt(NOW.minusSeconds(1));
        mockOwnedBooking();
        org.mockito.Mockito.doAnswer(invocation -> {
            booking.setStatus(BookingStatus.EXPIRED);
            return null;
        }).when(seatHoldService).expirePendingBooking(booking);

        assertThatThrownBy(() -> service.process("QS-PAYMENT", new MockPaymentRequest(true, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
        verify(seatHoldService).expirePendingBooking(booking);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void repeatedSuccessfulRequestIsIdempotentWhenStateIsConsistent() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(NOW);
        Payment successfulPayment = payment(PaymentStatus.SUCCESS);
        successfulPayment.setPaidAt(NOW);
        heldSeat.setStatus(SeatInventoryStatus.BOOKED);
        heldSeat.setHeldByBooking(null);
        heldSeat.setHoldExpiresAt(null);
        mockOwnedBooking();
        when(paymentRepository.findByBookingId(5L)).thenReturn(Optional.of(successfulPayment));
        when(bookingSeatRepository.findByBookingIdWithSeats(5L)).thenReturn(List.of(snapshot));
        when(showtimeSeatRepository.findBookingSeatsForUpdate(4L, List.of(6L)))
                .thenReturn(List.of(heldSeat));

        var result = service.process("QS-PAYMENT", new MockPaymentRequest(true, null));

        assertThat(result.alreadyProcessed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void inconsistentDuplicateSuccessIsRejected() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(NOW);
        Payment successfulPayment = payment(PaymentStatus.SUCCESS);
        successfulPayment.setPaidAt(NOW);
        mockOwnedBooking();
        when(paymentRepository.findByBookingId(5L)).thenReturn(Optional.of(successfulPayment));
        when(bookingSeatRepository.findByBookingIdWithSeats(5L)).thenReturn(List.of(snapshot));
        when(showtimeSeatRepository.findBookingSeatsForUpdate(4L, List.of(6L)))
                .thenReturn(List.of(heldSeat));

        assertThatThrownBy(() -> service.process("QS-PAYMENT", new MockPaymentRequest(true, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("seat state");
    }

    @Test
    void rejectsPaymentAmountMismatch() {
        mockOwnedBooking();

        assertThatThrownBy(() -> service.process("QS-PAYMENT",
                new MockPaymentRequest(true, new BigDecimal("4999.00"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsIncompleteSeatHold() {
        mockOwnedBooking();
        when(bookingSeatRepository.findByBookingIdWithSeats(5L)).thenReturn(List.of(snapshot));
        when(showtimeSeatRepository.findHeldSeatsByBookingIdForUpdate(5L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.process("QS-PAYMENT", new MockPaymentRequest(true, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("incomplete");
    }

    @Test
    void cannotPayAnotherCustomersBooking() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(bookingRepository.findCustomerBookingForUpdate("QS-OTHER", 50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.process("QS-OTHER", new MockPaymentRequest(true, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void transactionProxyRollsBackWhenFinalPaymentFlushFails() {
        mockValidPayment(null);
        RuntimeException databaseFailure = new RuntimeException("database write failed");
        org.mockito.Mockito.doThrow(databaseFailure).when(paymentRepository).flush();
        PlatformTransactionManager transactionManager = org.mockito.Mockito.mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = org.mockito.Mockito.mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        TransactionInterceptor interceptor = new TransactionInterceptor(transactionManager,
                new AnnotationTransactionAttributeSource());
        ProxyFactory proxyFactory = new ProxyFactory(service);
        proxyFactory.addAdvice(interceptor);
        MockPaymentService transactionalService = (MockPaymentService) proxyFactory.getProxy();

        assertThatThrownBy(() -> transactionalService.process("QS-PAYMENT",
                new MockPaymentRequest(true, null))).isSameAs(databaseFailure);

        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(transactionStatus);
    }

    private void mockOwnedBooking() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(bookingRepository.findCustomerBookingForUpdate("QS-PAYMENT", 50L))
                .thenReturn(Optional.of(booking));
    }

    private void mockValidPayment(Payment existingPayment) {
        mockOwnedBooking();
        when(paymentRepository.findByBookingId(5L)).thenReturn(Optional.ofNullable(existingPayment));
        when(bookingSeatRepository.findByBookingIdWithSeats(5L)).thenReturn(List.of(snapshot));
        when(showtimeSeatRepository.findHeldSeatsByBookingIdForUpdate(5L)).thenReturn(List.of(heldSeat));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(8L);
            return payment;
        });
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(8L);
        payment.setBooking(booking);
        payment.setPaymentReference("PAY-TEST");
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("5000.00"));
        return payment;
    }
}
