package com.quickseat.repository;

import com.quickseat.entity.Screen;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
    List<Screen> findByCinemaIdOrderByNameAsc(Long cinemaId);
    boolean existsByCinemaIdAndNameIgnoreCase(Long cinemaId, String name);
    boolean existsByCinemaIdAndNameIgnoreCaseAndIdNot(Long cinemaId, String name, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select screen from Screen screen join fetch screen.cinema where screen.id = :id")
    Optional<Screen> findByIdForUpdate(@Param("id") Long id);
}
