package com.quickseat.repository;

import com.quickseat.entity.Cinema;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CinemaRepository extends JpaRepository<Cinema, Long>, JpaSpecificationExecutor<Cinema> {
    Optional<Cinema> findByIdAndActiveTrue(Long id);
}
