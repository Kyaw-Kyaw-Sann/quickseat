package com.quickseat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "screens", uniqueConstraints = @UniqueConstraint(columnNames = {"cinema_id", "name"}))
public class Screen {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cinema_id", nullable = false) private Cinema cinema;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false) private boolean active = true;
    @CreationTimestamp @Column(nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(nullable = false) private Instant updatedAt;
}
