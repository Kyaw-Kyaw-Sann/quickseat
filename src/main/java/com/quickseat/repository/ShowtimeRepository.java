package com.quickseat.repository;

import com.quickseat.entity.Showtime;
import com.quickseat.entity.enums.ShowtimeStatus;
import java.time.Instant;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long>, JpaSpecificationExecutor<Showtime> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select showtime from Showtime showtime
            join fetch showtime.movie
            join fetch showtime.screen screen
            join fetch screen.cinema
            where showtime.id = :id
            """)
    Optional<Showtime> findByIdForInventoryGeneration(@Param("id") Long id);

    boolean existsByScreenIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long screenId, ShowtimeStatus status, Instant newEnd, Instant newStart);

    boolean existsByScreenIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            Long screenId, ShowtimeStatus status, Instant newEnd, Instant newStart, Long excludedId);
}
