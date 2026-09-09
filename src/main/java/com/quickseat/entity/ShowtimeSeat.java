package com.quickseat.entity;

import com.quickseat.entity.enums.SeatInventoryStatus;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "showtime_seats", uniqueConstraints = @UniqueConstraint(columnNames = {"showtime_id", "seat_id"}))
public class ShowtimeSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "showtime_id", nullable = false) private Showtime showtime;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "seat_id", nullable = false) private Seat seat;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SeatInventoryStatus status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "held_by_booking_id") private Booking heldByBooking;
    private Instant holdExpiresAt;
    @CreationTimestamp @Column(nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(nullable = false) private Instant updatedAt;
}
