package com.quickseat.service.seat;

import com.quickseat.dto.request.seat.SeatLayoutRequest;
import com.quickseat.dto.request.seat.SeatRowRequest;
import com.quickseat.dto.request.seat.SeatUpdateRequest;
import com.quickseat.dto.response.seat.SeatResponse;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Seat;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.exception.BadRequestException;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.ScreenRepository;
import com.quickseat.repository.SeatRepository;
import com.quickseat.service.screen.ScreenService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {
    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;

    @Transactional
    public List<SeatResponse> generateLayout(Long screenId, SeatLayoutRequest request) {
        Screen screen = getScreen(screenId);
        requireOperational(screen);
        if (seatRepository.existsByScreenId(screenId)) throw new ConflictException("Seat layout already exists for this screen");
        Set<String> rowNames = new HashSet<>();
        List<Seat> seats = new ArrayList<>();
        for (SeatRowRequest row : request.rows()) {
            String rowName = row.rowName().trim().toUpperCase(Locale.ROOT);
            if (!rowNames.add(rowName)) throw new ConflictException("Duplicate row in seat layout: " + rowName);
            if (row.normalSeats() + row.coupleSeats() == 0) throw new BadRequestException("Each row must contain at least one seat");
            int number = 1;
            for (int i = 0; i < row.normalSeats(); i++) seats.add(newSeat(screen, rowName, number++, SeatType.NORMAL));
            for (int i = 0; i < row.coupleSeats(); i++) seats.add(newSeat(screen, rowName, number++, SeatType.COUPLE));
        }
        List<Seat> saved = seatRepository.saveAll(seats);
        log.info("Generated {} seats for screen {}", saved.size(), screenId);
        return saved.stream().map(this::toResponse).toList();
    }

    public List<SeatResponse> list(Long screenId) {
        if (!screenRepository.existsById(screenId)) throw new ResourceNotFoundException("Screen not found");
        return seatRepository.findByScreenIdOrderByRowNameAscSeatNumberAsc(screenId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SeatResponse update(Long id, SeatUpdateRequest request) {
        Seat seat = getEntity(id);
        String rowName = request.rowName().trim().toUpperCase(Locale.ROOT);
        if (seatRepository.existsByScreenIdAndRowNameIgnoreCaseAndSeatNumberAndIdNot(
                seat.getScreen().getId(), rowName, request.seatNumber(), id)) {
            throw new ConflictException("Seat position already exists in this screen");
        }
        seat.setRowName(rowName);
        seat.setSeatNumber(request.seatNumber());
        seat.setSeatType(request.seatType());
        return toResponse(seat);
    }

    @Transactional
    public SeatResponse setActive(Long id, boolean active) {
        Seat seat = getEntity(id);
        if (active) requireOperational(seat.getScreen());
        seat.setActive(active);
        log.info("Seat {} active status changed to {}", id, active);
        return toResponse(seat);
    }

    private Seat newSeat(Screen screen, String rowName, int number, SeatType type) {
        Seat seat = new Seat(); seat.setScreen(screen); seat.setRowName(rowName);
        seat.setSeatNumber(number); seat.setSeatType(type); return seat;
    }

    private Screen getScreen(Long id) {
        return screenRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Screen not found"));
    }

    private Seat getEntity(Long id) {
        return seatRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
    }

    private void requireOperational(Screen screen) {
        if (!screen.isActive() || !screen.getCinema().isActive()) throw new BadRequestException("Screen and cinema must be active");
    }

    private SeatResponse toResponse(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getScreen().getId(), seat.getRowName(), seat.getSeatNumber(),
                seat.getSeatType(), seat.isActive(), seat.getCreatedAt(), seat.getUpdatedAt());
    }
}
