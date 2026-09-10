package com.quickseat.repository;

import com.quickseat.entity.Booking;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
    Optional<Booking> findByBookingReference(String bookingReference);

    boolean existsByBookingReference(String bookingReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select booking from Booking booking
            join fetch booking.showtime showtime
            join fetch showtime.movie
            join fetch showtime.screen screen
            join fetch screen.cinema
            where booking.bookingReference = :bookingReference
              and booking.user.id = :userId
            """)
    Optional<Booking> findCustomerBookingForUpdate(@Param("bookingReference") String bookingReference,
                                                    @Param("userId") Long userId);
}
