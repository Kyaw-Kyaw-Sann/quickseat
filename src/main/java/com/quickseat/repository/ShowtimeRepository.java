package com.quickseat.repository;

import com.quickseat.entity.Showtime;
import com.quickseat.entity.enums.ShowtimeStatus;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long>, JpaSpecificationExecutor<Showtime> {
    boolean existsByScreenIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long screenId, ShowtimeStatus status, Instant newEnd, Instant newStart);

    boolean existsByScreenIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            Long screenId, ShowtimeStatus status, Instant newEnd, Instant newStart, Long excludedId);
}
