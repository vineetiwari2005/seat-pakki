package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Enums.TicketStatus;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TicketRepository - Data access for tickets
 * 
 * Design:
 * - User-specific booking history queries
 * - Status-aware queries (BOOKED vs CANCELLED)
 * - Pagination support for large datasets
 * - Sorting by booking time (latest first)
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket,Integer> {

    /**
     * Find all tickets for a user, ordered by booking time descending
     * Latest bookings first
     */
    List<Ticket> findByUserOrderByBookedAtDesc(User user);

    /**
     * Find active (BOOKED) tickets for a user
     */
    List<Ticket> findByUserAndStatusOrderByBookedAtDesc(User user, TicketStatus status);

    /**
     * Find paginated tickets for a user
     */
    Page<Ticket> findByUser(User user, Pageable pageable);

    /**
     * Find tickets booked after a certain date
     */
    List<Ticket> findByUserAndBookedAtAfterOrderByBookedAtDesc(
        User user, 
        LocalDateTime afterDate
    );

    /**
     * Count total bookings for a user
     */
    long countByUser(User user);

    /**
     * Count active bookings for a user
     */
    long countByUserAndStatus(User user, TicketStatus status);

    /**
     * Find user's upcoming shows (show date in future), only BOOKED tickets
     */
    @Query("SELECT t FROM Ticket t " +
           "WHERE t.user = :user " +
           "AND t.status = 'BOOKED' " +
           "AND t.show.date >= CURRENT_DATE " +
           "ORDER BY t.show.date ASC, t.show.time ASC")
    List<Ticket> findUpcomingTicketsByUser(@Param("user") User user);

    /**
     * Find user's past shows (includes both BOOKED and CANCELLED for history)
     */
    @Query("SELECT t FROM Ticket t " +
           "WHERE t.user = :user " +
           "AND t.show.date < CURRENT_DATE " +
           "ORDER BY t.show.date DESC, t.show.time DESC")
    List<Ticket> findPastTicketsByUser(@Param("user") User user);
}

