package com.quickseat.service.showtime;

import com.quickseat.dto.response.showtime.ShowtimeSeatMapResponse;
import com.quickseat.dto.response.showtime.ShowtimeSeatResponse;
import com.quickseat.entity.Seat;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.exception.BadRequestException;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.SeatRepository;
import com.quickseat.repository.ShowtimeRepository;
import com.quickseat.repository.ShowtimeSeatRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeSeatInventoryService {
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;

    @Transactional
    public ShowtimeSeatMapResponse generate(Long showtimeId) {
        Showtime showtime = showtimeRepository.findByIdForInventoryGeneration(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
        validateShowtimeForGeneration(showtime);

        if (showtimeSeatRepository.existsByShowtimeId(showtimeId)) {
            throw new ConflictException("Seat inventory has already been generated for this showtime");
        }

        List<Seat> screenSeats = seatRepository
                .findByScreenIdOrderByRowNameAscSeatNumberAsc(showtime.getScreen().getId());
        if (screenSeats.isEmpty()) {
            throw new BadRequestException("Cannot generate inventory because the screen has no seats");
        }

        List<ShowtimeSeat> inventory = screenSeats.stream()
                .map(seat -> createInventorySeat(showtime, seat))
                .toList();
        List<ShowtimeSeat> savedInventory = showtimeSeatRepository.saveAll(inventory);
        log.info("Generated {} inventory seats for showtime {}", savedInventory.size(), showtimeId);
        return toSeatMap(showtime, savedInventory);
    }

    @Transactional(readOnly = true)
    public ShowtimeSeatMapResponse getSeatMap(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
        List<ShowtimeSeat> inventory = showtimeSeatRepository.findSeatMapByShowtimeId(showtimeId);
        if (inventory.isEmpty()) {
            throw new ResourceNotFoundException("Seat inventory has not been generated for this showtime");
        }
        return toSeatMap(showtime, inventory);
    }

    private void validateShowtimeForGeneration(Showtime showtime) {
        if (showtime.getStatus() != ShowtimeStatus.ACTIVE) {
            throw new BadRequestException("Seat inventory can only be generated for an active showtime");
        }
        if (!showtime.getMovie().isActive()) {
            throw new BadRequestException("Cannot generate inventory for an inactive movie");
        }
        if (!showtime.getScreen().isActive()) {
            throw new BadRequestException("Cannot generate inventory for an inactive screen");
        }
        if (!showtime.getScreen().getCinema().isActive()) {
            throw new BadRequestException("Cannot generate inventory for an inactive cinema");
        }
    }

    private ShowtimeSeat createInventorySeat(Showtime showtime, Seat seat) {
        ShowtimeSeat showtimeSeat = new ShowtimeSeat();
        showtimeSeat.setShowtime(showtime);
        showtimeSeat.setSeat(seat);
        showtimeSeat.setStatus(seat.isActive()
                ? SeatInventoryStatus.AVAILABLE
                : SeatInventoryStatus.UNAVAILABLE);
        return showtimeSeat;
    }

    private ShowtimeSeatMapResponse toSeatMap(Showtime showtime, List<ShowtimeSeat> inventory) {
        List<ShowtimeSeatResponse> seats = inventory.stream()
                .map(showtimeSeat -> toSeatResponse(showtime, showtimeSeat))
                .toList();
        return new ShowtimeSeatMapResponse(showtime.getId(), showtime.getMovie().getTitle(),
                showtime.getScreen().getName(), showtime.getStartTime(), seats);
    }

    private ShowtimeSeatResponse toSeatResponse(Showtime showtime, ShowtimeSeat showtimeSeat) {
        Seat seat = showtimeSeat.getSeat();
        BigDecimal price = seat.getSeatType() == SeatType.COUPLE
                ? showtime.getCouplePrice()
                : showtime.getNormalPrice();
        return new ShowtimeSeatResponse(showtimeSeat.getId(), seat.getId(), seat.getRowName(),
                seat.getSeatNumber(), seat.getSeatType(), price, showtimeSeat.getStatus());
    }
}
