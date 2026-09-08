package com.quickseat.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "booking_seats", uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id", "seat_id"}))
public class BookingSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "booking_id", nullable = false) private Booking booking;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "seat_id", nullable = false) private Seat seat;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SeatType seatType;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
}
