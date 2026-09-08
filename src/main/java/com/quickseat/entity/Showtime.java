package com.quickseat.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "showtimes")
public class Showtime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "movie_id", nullable = false) private Movie movie;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "screen_id", nullable = false) private Screen screen;
    @Column(nullable = false) private Instant startTime;
    @Column(nullable = false) private Instant endTime;
    @Column(nullable = false) private Integer cleaningBufferMinutes = 15;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal normalPrice;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal couplePrice;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ShowtimeStatus status;
    @CreationTimestamp @Column(nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(nullable = false) private Instant updatedAt;
}
