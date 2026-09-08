package com.quickseat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "cinemas")
public class Cinema {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 150) private String name;
    @Column(nullable = false, columnDefinition = "TEXT") private String address;
    @Column(nullable = false, length = 100) private String city;
    @Column(length = 30) private String phone;
    @Column(columnDefinition = "TEXT") private String imageUrl;
    @Column(nullable = false) private boolean active = true;
    @CreationTimestamp @Column(nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(nullable = false) private Instant updatedAt;
}
