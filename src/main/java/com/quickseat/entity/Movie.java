package com.quickseat.entity;

import com.quickseat.entity.enums.MovieStatus;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "movies")
public class Movie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 200) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false) private Integer durationMinutes;
    private LocalDate releaseDate;
    @Column(length = 50) private String language;
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(columnDefinition = "TEXT[]") private String[] genres;
    @Column(length = 20) private String ageRating;
    @Column(length = 150) private String director;
    @Column(columnDefinition = "TEXT") private String castText;
    @Column(columnDefinition = "TEXT") private String posterUrl;
    @Column(columnDefinition = "TEXT") private String trailerUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MovieStatus status;
    @Column(nullable = false) private boolean active = true;
    @CreationTimestamp @Column(nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(nullable = false) private Instant updatedAt;
}
