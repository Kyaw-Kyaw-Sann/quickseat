package com.quickseat.entity;

import com.quickseat.entity.enums.SeatType;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "seats", uniqueConstraints = @UniqueConstraint(columnNames = {"screen_id", "row_name", "seat_number"}))
public class Seat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "screen_id", nullable = false) private Screen screen;
    @Column(name = "row_name", nullable = false, length = 10) private String rowName;
    @Column(name = "seat_number", nullable = false) private Integer seatNumber;
    @Enumerated(EnumType.STRING) @Column(name = "seat_type", nullable = false, length = 20) private SeatType seatType;
    @Column(nullable = false) private boolean active = true;
    @CreationTimestamp @Column(nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(nullable = false) private Instant updatedAt;
}
