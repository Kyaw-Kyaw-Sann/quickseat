package com.quickseat.repository;

import com.quickseat.entity.EmailVerificationToken;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    List<EmailVerificationToken> findAllByUserAndUsedFalse(com.quickseat.entity.User user);
}
