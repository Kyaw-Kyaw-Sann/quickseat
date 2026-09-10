package com.quickseat.repository;

import com.quickseat.entity.BookingSeat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    @Query("""
            select bookingSeat from BookingSeat bookingSeat
            join fetch bookingSeat.seat seat
            where bookingSeat.booking.id = :bookingId
            order by seat.rowName asc, seat.seatNumber asc
            """)
    List<BookingSeat> findByBookingIdWithSeats(@Param("bookingId") Long bookingId);

    @Query("""
            select bookingSeat from BookingSeat bookingSeat
            join fetch bookingSeat.seat seat
            where bookingSeat.booking.id in :bookingIds
            order by bookingSeat.booking.id, seat.rowName asc, seat.seatNumber asc
            """)
    List<BookingSeat> findByBookingIdsWithSeats(@Param("bookingIds") List<Long> bookingIds);
}
