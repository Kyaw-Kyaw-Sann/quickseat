package com.quickseat.repository;

import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.enums.SeatInventoryStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    boolean existsByShowtimeId(Long showtimeId);

    @Query("""
            select showtimeSeat from ShowtimeSeat showtimeSeat
            join fetch showtimeSeat.seat seat
            where showtimeSeat.showtime.id = :showtimeId
            order by seat.rowName asc, seat.seatNumber asc
            """)
    List<ShowtimeSeat> findSeatMapByShowtimeId(@Param("showtimeId") Long showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select showtimeSeat from ShowtimeSeat showtimeSeat
            join fetch showtimeSeat.seat seat
            where showtimeSeat.showtime.id = :showtimeId
              and showtimeSeat.id in :showtimeSeatIds
            order by showtimeSeat.id asc
            """)
    List<ShowtimeSeat> findSelectedSeatsForUpdate(@Param("showtimeId") Long showtimeId,
                                                   @Param("showtimeSeatIds") List<Long> showtimeSeatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select showtimeSeat from ShowtimeSeat showtimeSeat
            join fetch showtimeSeat.seat
            where showtimeSeat.heldByBooking.id = :bookingId
            order by showtimeSeat.id asc
            """)
    List<ShowtimeSeat> findHeldSeatsByBookingIdForUpdate(@Param("bookingId") Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select showtimeSeat from ShowtimeSeat showtimeSeat
            join fetch showtimeSeat.seat
            join fetch showtimeSeat.heldByBooking
            where showtimeSeat.status = :status
              and showtimeSeat.holdExpiresAt <= :now
            order by showtimeSeat.id asc
            """)
    List<ShowtimeSeat> findExpiredHoldsForUpdate(@Param("status") SeatInventoryStatus status,
                                                  @Param("now") Instant now);

    @Query("""
            select showtimeSeat from ShowtimeSeat showtimeSeat
            join fetch showtimeSeat.seat seat
            where showtimeSeat.showtime.id = :showtimeId
              and seat.id in :seatIds
            order by seat.rowName asc, seat.seatNumber asc
            """)
    List<ShowtimeSeat> findByShowtimeIdAndSeatIds(@Param("showtimeId") Long showtimeId,
                                                   @Param("seatIds") List<Long> seatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select showtimeSeat from ShowtimeSeat showtimeSeat
            join fetch showtimeSeat.seat seat
            where showtimeSeat.showtime.id = :showtimeId
              and seat.id in :seatIds
            order by showtimeSeat.id asc
            """)
    List<ShowtimeSeat> findBookingSeatsForUpdate(@Param("showtimeId") Long showtimeId,
                                                  @Param("seatIds") List<Long> seatIds);
}
