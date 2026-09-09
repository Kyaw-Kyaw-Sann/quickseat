package com.quickseat.dto.request.movie;

import com.quickseat.entity.enums.MovieStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record MovieRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotNull @Positive Integer durationMinutes,
        LocalDate releaseDate,
        @Size(max = 50) String language,
        List<@NotBlank @Size(max = 50) String> genres,
        @Size(max = 20) String ageRating,
        @Size(max = 150) String director,
        String castText,
        @Pattern(regexp = "^$|^https?://.+$", message = "posterUrl must be a valid HTTP or HTTPS URL") String posterUrl,
        @Pattern(regexp = "^$|^https?://.+$", message = "trailerUrl must be a valid HTTP or HTTPS URL") String trailerUrl,
        @NotNull MovieStatus status
) { }
