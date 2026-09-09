package com.quickseat.entity;

import com.quickseat.entity.enums.TicketStatus;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "tickets")
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "booking_id", nullable = false, unique = true) private Booking booking;
    @Column(nullable = false, unique = true) private String ticketToken;
    @Column(columnDefinition = "TEXT") private String qrImageUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TicketStatus status;
    private Instant usedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "verified_by") private User verifiedBy;
    @CreationTimestamp @Column(nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(nullable = false) private Instant updatedAt;
}
