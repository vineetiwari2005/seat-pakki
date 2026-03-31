package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.ShowSeat;
import com.driver.bookMyShow.Models.Show;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Integer> {
    
    /**
     * ATOMIC UPDATE - Mark seats as unavailable
     * Prevents race conditions by using database-level locking
     * Returns number of rows updated (0 if seat already booked)
     */
    @Modifying
    @Query("UPDATE ShowSeat s SET s.isAvailable = false " +
           "WHERE s.show.id = :showId " +
           "AND s.seatNo IN :seatNumbers " +
           "AND s.isAvailable = true")
    int markSeatsAsUnavailable(@Param("showId") Integer showId, 
                               @Param("seatNumbers") List<String> seatNumbers);
    
    /**
     * ATOMIC UPDATE - Mark seats as available (for cancellation)
     */
    @Modifying
    @Query("UPDATE ShowSeat s SET s.isAvailable = true " +
           "WHERE s.show.id = :showId " +
           "AND s.seatNo IN :seatNumbers")
    int markSeatsAsAvailable(@Param("showId") Integer showId, 
                             @Param("seatNumbers") List<String> seatNumbers);
    
    /**
     * Find seats by show and seat numbers
     */
    @Query("SELECT s FROM ShowSeat s " +
           "WHERE s.show = :show " +
           "AND s.seatNo IN :seatNumbers")
    List<ShowSeat> findByShowAndSeatNoIn(@Param("show") Show show, 
                                         @Param("seatNumbers") List<String> seatNumbers);

    /**
     * Pessimistic lock seats for concurrent booking protection.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ShowSeat s " +
           "WHERE s.show = :show " +
           "AND s.seatNo IN :seatNumbers")
    List<ShowSeat> findByShowAndSeatNoInForUpdate(@Param("show") Show show,
                                                  @Param("seatNumbers") List<String> seatNumbers);
    
    /**
     * Find a specific seat by seat number and show
     */
    @Query("SELECT s FROM ShowSeat s " +
           "WHERE s.show = :show " +
           "AND s.seatNo = :seatNo")
    List<ShowSeat> findBySeatNoAndShow(@Param("seatNo") String seatNo, 
                                       @Param("show") Show show);
    
    /**
     * Count available seats for a show
     */
    @Query("SELECT COUNT(s) FROM ShowSeat s WHERE s.show = :show AND s.isAvailable = true")
    long countAvailableSeats(@Param("show") Show show);
    
    /**
     * Count total seats for a show
     */
    long countByShow(Show show);

    /**
     * Find all seats for a show (ordered by seat number)
     */
    @Query("SELECT s FROM ShowSeat s WHERE s.show.id = :showId ORDER BY s.seatNo")
    List<ShowSeat> findByShowId(@Param("showId") Integer showId);
}
