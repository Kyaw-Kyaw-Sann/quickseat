package com.quickseat.dto.response.movie;

import com.quickseat.entity.enums.MovieStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MovieResponse(
        Long id,
        String title,
        String description,
        Integer durationMinutes,
        LocalDate releaseDate,
        String language,
        List<String> genres,
        String ageRating,
        String director,
        String castText,
        String posterUrl,
        String trailerUrl,
        MovieStatus status,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) { }
