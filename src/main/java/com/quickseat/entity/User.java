package com.quickseat.entity;

import com.quickseat.entity.enums.AuthProvider;
import com.quickseat.entity.enums.Role;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, unique = true) private String email;
    private String password;
    @Column(length = 30) private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cinema_id") private Cinema cinema;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AuthProvider provider;
    @Column(nullable = false) private boolean emailVerified;
    @Column(nullable = false) private boolean active = true;
    @CreationTimestamp @Column(nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(nullable = false) private Instant updatedAt;
}
