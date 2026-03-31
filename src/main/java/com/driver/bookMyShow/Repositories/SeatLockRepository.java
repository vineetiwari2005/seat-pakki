package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Enums.SeatLockStatus;
import com.driver.bookMyShow.Models.SeatLock;
import com.driver.bookMyShow.Models.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SeatLockRepository - Data access for seat lock operations
 */
@Repository
public interface SeatLockRepository extends JpaRepository<SeatLock, Integer> {

    /**
     * Find active lock for a specific seat in a show
     */
    @Query("SELECT sl FROM SeatLock sl WHERE sl.show.id = :showId " +
           "AND sl.seatNumber = :seatNumber " +
           "AND sl.status = 'LOCKED' " +
           "AND sl.expiryTime > :currentTime")
    Optional<SeatLock> findActiveLock(@Param("showId") Integer showId,
                                       @Param("seatNumber") String seatNumber,
                                       @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find all active locks for a show
     */
    @Query("SELECT sl FROM SeatLock sl WHERE sl.show.id = :showId " +
           "AND sl.status = 'LOCKED' " +
           "AND sl.expiryTime > :currentTime")
    List<SeatLock> findActiveLocksForShow(@Param("showId") Integer showId,
                                           @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find all locks for a user in a specific show
     */
    List<SeatLock> findByShowAndUser(Show show, com.driver.bookMyShow.Models.User user);

    /**
     * Find locks by session ID
     */
    List<SeatLock> findBySessionId(String sessionId);

    /**
     * Find expired locks
     */
    @Query("SELECT sl FROM SeatLock sl WHERE sl.status = 'LOCKED' " +
           "AND sl.expiryTime <= :currentTime")
    List<SeatLock> findExpiredLocks(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Release expired locks (bulk update)
     */
    @Modifying
    @Query("UPDATE SeatLock sl SET sl.status = 'RELEASED' " +
           "WHERE sl.status = 'LOCKED' " +
           "AND sl.expiryTime <= :currentTime")
    int releaseExpiredLocks(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Count active locks for a user
     */
    @Query("SELECT COUNT(sl) FROM SeatLock sl WHERE sl.user.id = :userId " +
           "AND sl.status = 'LOCKED' " +
           "AND sl.expiryTime > :currentTime")
    long countActiveLocksForUser(@Param("userId") Integer userId,
                                  @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find lock by session and seat
     */
    Optional<SeatLock> findBySessionIdAndSeatNumber(String sessionId, String seatNumber);

    /**
     * Find all locks for a show and seat number (regardless of status)
     */
    @Query("SELECT sl FROM SeatLock sl WHERE sl.show.id = :showId " +
           "AND sl.seatNumber = :seatNumber")
    List<SeatLock> findByShowIdAndSeatNumber(@Param("showId") Integer showId,
                                              @Param("seatNumber") String seatNumber);
    
    /**
     * Delete expired locks for specific seat to prevent duplicate entries
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SeatLock sl WHERE sl.show.id = :showId " +
           "AND sl.seatNumber = :seatNumber " +
           "AND sl.expiryTime < :now")
    void deleteExpiredLocksForSeat(@Param("showId") Integer showId, 
                                    @Param("seatNumber") String seatNumber,
                                    @Param("now") LocalDateTime now);
    
    /**
     * Delete all expired locks
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SeatLock sl WHERE sl.expiryTime < :now")
    void deleteAllExpiredLocks(@Param("now") LocalDateTime now);
    
    /**
     * Delete all old locks (RELEASED/CONFIRMED) for specific seat to prevent duplicates
     * This ensures we can create new locks even if old ones exist
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SeatLock sl WHERE sl.show.id = :showId " +
           "AND sl.seatNumber = :seatNumber " +
           "AND sl.status IN ('RELEASED', 'CONFIRMED')")
    void deleteOldLocksForSeat(@Param("showId") Integer showId, 
                                @Param("seatNumber") String seatNumber);
    
    /**
     * Delete all old locks for multiple seats in a show
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SeatLock sl WHERE sl.show.id = :showId " +
           "AND sl.seatNumber IN :seatNumbers " +
           "AND sl.status IN ('RELEASED', 'CONFIRMED')")
    void deleteOldLocksForSeats(@Param("showId") Integer showId, 
                                 @Param("seatNumbers") List<String> seatNumbers);
}
