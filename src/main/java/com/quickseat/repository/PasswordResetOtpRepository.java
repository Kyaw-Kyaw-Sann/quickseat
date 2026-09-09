package com.quickseat.repository;

import com.quickseat.entity.PasswordResetOtp;
import com.quickseat.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findTopByUserOrderByCreatedAtDesc(User user);
}
