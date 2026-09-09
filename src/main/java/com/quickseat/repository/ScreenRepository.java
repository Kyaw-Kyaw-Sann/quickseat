package com.quickseat.repository;

import com.quickseat.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
    List<Screen> findByCinemaIdOrderByNameAsc(Long cinemaId);
    boolean existsByCinemaIdAndNameIgnoreCase(Long cinemaId, String name);
    boolean existsByCinemaIdAndNameIgnoreCaseAndIdNot(Long cinemaId, String name, Long id);
}
