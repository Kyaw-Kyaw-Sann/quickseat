package com.quickseat.repository;

import com.quickseat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScreenIdOrderByRowNameAscSeatNumberAsc(Long screenId);
    boolean existsByScreenId(Long screenId);
    boolean existsByScreenIdAndRowNameIgnoreCaseAndSeatNumber(Long screenId, String rowName, Integer seatNumber);
    boolean existsByScreenIdAndRowNameIgnoreCaseAndSeatNumberAndIdNot(Long screenId, String rowName, Integer seatNumber, Long id);
}
