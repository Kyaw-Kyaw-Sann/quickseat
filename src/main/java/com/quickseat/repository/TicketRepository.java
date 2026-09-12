package com.quickseat.repository;

import com.quickseat.entity.Ticket;
import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketToken(String ticketToken);

    Optional<Ticket> findByBookingId(Long bookingId);

    boolean existsByTicketToken(String ticketToken);

    List<Ticket> findByBookingIdIn(List<Long> bookingIds);

    @Query("""
            select ticket from Ticket ticket
            join fetch ticket.booking booking
            join fetch booking.showtime showtime
            join fetch showtime.movie
            join fetch showtime.screen screen
            join fetch screen.cinema
            where ticket.ticketToken = :ticketToken
              and booking.user.id = :userId
            """)
    Optional<Ticket> findCustomerTicketByToken(@Param("ticketToken") String ticketToken,
                                                @Param("userId") Long userId);

    @Query("""
            select ticket from Ticket ticket
            join fetch ticket.booking booking
            join fetch booking.user
            join fetch booking.showtime showtime
            join fetch showtime.movie
            join fetch showtime.screen screen
            join fetch screen.cinema
            where ticket.ticketToken = :ticketToken
            """)
    Optional<Ticket> findByTicketTokenWithDetails(@Param("ticketToken") String ticketToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ticket from Ticket ticket
            join fetch ticket.booking booking
            join fetch booking.user
            join fetch booking.showtime showtime
            join fetch showtime.movie
            join fetch showtime.screen screen
            join fetch screen.cinema
            where ticket.ticketToken = :ticketToken
            """)
    Optional<Ticket> findByTicketTokenForUpdate(@Param("ticketToken") String ticketToken);
}
