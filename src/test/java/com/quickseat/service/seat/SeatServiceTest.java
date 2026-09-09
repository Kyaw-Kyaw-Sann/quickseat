package com.quickseat.service.seat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.quickseat.dto.request.seat.SeatLayoutRequest;
import com.quickseat.dto.request.seat.SeatRowRequest;
import com.quickseat.dto.request.seat.SeatUpdateRequest;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Seat;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.exception.ConflictException;
import com.quickseat.repository.ScreenRepository;
import com.quickseat.repository.SeatRepository;
import com.quickseat.service.screen.ScreenService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {
    @Mock SeatRepository seatRepository;
    @Mock ScreenRepository screenRepository;
    SeatService service;
    Screen screen;

    @BeforeEach void setUp() {
        service = new SeatService(seatRepository, screenRepository);
        Cinema cinema = new Cinema(); cinema.setId(1L); cinema.setActive(true);
        screen = new Screen(); screen.setId(2L); screen.setCinema(cinema); screen.setActive(true);
    }

    @Test void generatesNormalAndCoupleSeats() {
        when(screenRepository.findById(2L)).thenReturn(Optional.of(screen));
        when(seatRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Seat> seats = invocation.getArgument(0); AtomicLong id = new AtomicLong(1);
            seats.forEach(seat -> seat.setId(id.getAndIncrement())); return seats;
        });
        var result = service.generateLayout(2L, new SeatLayoutRequest(List.of(new SeatRowRequest("A", 3, 2))));
        assertThat(result).hasSize(5);
        assertThat(result).extracting(seat -> seat.seatType()).containsExactly(
                SeatType.NORMAL, SeatType.NORMAL, SeatType.NORMAL, SeatType.COUPLE, SeatType.COUPLE);
    }

    @Test void rejectsDuplicateRowsAndDuplicateEditedPosition() {
        when(screenRepository.findById(2L)).thenReturn(Optional.of(screen));
        assertThatThrownBy(() -> service.generateLayout(2L,
                new SeatLayoutRequest(List.of(new SeatRowRequest("A", 1, 0), new SeatRowRequest("a", 1, 0)))))
                .isInstanceOf(ConflictException.class);

        Seat seat = seat(10L, "A", 1);
        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        when(seatRepository.existsByScreenIdAndRowNameIgnoreCaseAndSeatNumberAndIdNot(2L, "A", 2, 10L)).thenReturn(true);
        assertThatThrownBy(() -> service.update(10L, new SeatUpdateRequest("A", 2, SeatType.NORMAL)))
                .isInstanceOf(ConflictException.class);
    }

    @Test void deactivatesSeat() {
        Seat seat = seat(10L, "A", 1);
        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        assertThat(service.setActive(10L, false).active()).isFalse();
    }

    private Seat seat(Long id, String row, int number) { Seat seat = new Seat(); seat.setId(id); seat.setScreen(screen); seat.setRowName(row); seat.setSeatNumber(number); seat.setSeatType(SeatType.NORMAL); seat.setActive(true); return seat; }
}
