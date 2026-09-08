package com.quickseat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "password_reset_otps")
public class PasswordResetOtp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 6) private String otp;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false) private boolean verified;
    @Column(nullable = false) private int attemptCount;
    @CreationTimestamp @Column(nullable = false, updatable = false) private Instant createdAt;
}
