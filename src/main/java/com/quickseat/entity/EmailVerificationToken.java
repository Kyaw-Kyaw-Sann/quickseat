package com.quickseat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "email_verification_tokens")
public class EmailVerificationToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, unique = true) private String token;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false) private boolean used;
    @CreationTimestamp @Column(nullable = false, updatable = false) private Instant createdAt;
}
