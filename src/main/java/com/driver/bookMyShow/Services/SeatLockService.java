package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Enums.SeatLockStatus;
import com.driver.bookMyShow.Models.SeatLock;
import com.driver.bookMyShow.Models.Show;
import com.driver.bookMyShow.Models.ShowSeat;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.SeatLockRepository;
import com.driver.bookMyShow.Repositories.ShowRepository;
import com.driver.bookMyShow.Repositories.ShowSeatRepository;
import com.driver.bookMyShow.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SeatLockService - Manages seat locking for race-condition-safe booking
 * 
 * Features:
 * - Temporary seat locking (15-minute expiry)
 * - Automatic lock release on timeout
 * - Prevention of double-booking
 * - Session-based lock management
 */
@Service
public class SeatLockService {

    @Autowired
    private SeatLockRepository seatLockRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ShowSeatRepository showSeatRepository;

    // Lock duration: 15 minutes
    private static final int LOCK_DURATION_MINUTES = 15;

    // Max seats per transaction
    private static final int MAX_SEATS_PER_USER = 10;

    /**
     * Lock seats for a user
     * Returns session ID if successful, throws exception if seats unavailable
     */
    @Transactional
    public synchronized String lockSeats(Integer showId, Integer userId, List<String> seatNumbers) 
            throws Exception {
        
        // Clean up all expired locks globally
        seatLockRepository.deleteAllExpiredLocks(LocalDateTime.now());
        
        // Delete old locks (RELEASED/CONFIRMED) for the specific seats we're trying to lock
        // This prevents unique constraint violations from previous bookings
        seatLockRepository.deleteOldLocksForSeats(showId, seatNumbers);
        
        // Validate show exists
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new Exception("Show not found"));

        // CRITICAL: Validate show hasn't started or passed
        // Use toLocalDate()/toLocalTime() to properly convert SQL Date/Time avoiding timezone issues
        LocalDateTime showDateTime = LocalDateTime.of(
            show.getDate().toLocalDate(),
            show.getTime().toLocalTime()
        );
        LocalDateTime now = LocalDateTime.now();
        
        // Debug logging
        System.out.println("DEBUG - Show validation:");
        System.out.println("  Show ID: " + showId);
        System.out.println("  Show Date (SQL): " + show.getDate());
        System.out.println("  Show Time (SQL): " + show.getTime());
        System.out.println("  Show DateTime (LocalDateTime): " + showDateTime);
        System.out.println("  Current DateTime: " + now);
        System.out.println("  Show is in past? " + showDateTime.isBefore(now));
        
        // Only check if show has already passed (started)
        // Allow booking up to show start time
        if (showDateTime.isBefore(now)) {
            throw new Exception("Cannot book tickets for a show that has already started or passed. Show time: " + showDateTime);
        }
        
        // Additional validation: Don't allow booking too close to show time (e.g., less than 30 minutes)
        if (showDateTime.minusMinutes(30).isBefore(now)) {
            throw new Exception("Cannot book tickets less than 30 minutes before show time. Show starts at: " + showDateTime);
        }

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        // Validate seat count
        if (seatNumbers.size() > MAX_SEATS_PER_USER) {
            throw new Exception("Cannot lock more than " + MAX_SEATS_PER_USER + " seats at once");
        }

        // Release expired locks first
        releaseExpiredLocks();

        // CRITICAL: Check if seats are already booked (not just locked)
        // This prevents booking the same seat multiple times
        List<ShowSeat> showSeats = showSeatRepository.findByShowAndSeatNoInForUpdate(show, seatNumbers);
        
        // Validate all requested seats exist
        if (showSeats.size() != seatNumbers.size()) {
            throw new Exception("One or more seats do not exist in this show");
        }
        
        // Check if any seat is already booked
        for (ShowSeat showSeat : showSeats) {
            if (!showSeat.getIsAvailable()) {
                throw new Exception("Seat " + showSeat.getSeatNo() + " is already booked");
            }
        }

        // Check if seats are locked by another user (reuse 'now' variable from above)
        for (String seatNumber : seatNumbers) {
            Optional<SeatLock> existingLock = seatLockRepository.findActiveLock(
                    showId, seatNumber, now);
            
            if (existingLock.isPresent()) {
                throw new Exception("Seat " + seatNumber + " is already locked by another user");
            }
        }

        // Delete any old RELEASED/CONFIRMED locks for these seats to avoid unique constraint violation
        for (String seatNumber : seatNumbers) {
            List<SeatLock> oldLocks = seatLockRepository.findByShowIdAndSeatNumber(showId, seatNumber);
            if (!oldLocks.isEmpty()) {
                seatLockRepository.deleteAll(oldLocks);
            }
        }

        // Create session ID for this booking attempt
        String sessionId = UUID.randomUUID().toString();

        // Lock all seats
        LocalDateTime expiryTime = now.plusMinutes(LOCK_DURATION_MINUTES);
        List<SeatLock> locks = new ArrayList<>();

        for (String seatNumber : seatNumbers) {
            SeatLock lock = SeatLock.builder()
                    .show(show)
                    .seatNumber(seatNumber)
                    .user(user)
                    .lockTime(now)
                    .expiryTime(expiryTime)
                    .status(SeatLockStatus.LOCKED)
                    .sessionId(sessionId)
                    .build();
            locks.add(lock);
        }

        seatLockRepository.saveAll(locks);
        return sessionId;
    }

    /**
     * Release locks for a session (on payment failure or user cancellation)
     */
    @Transactional
    public void releaseLocks(String sessionId) {
        List<SeatLock> locks = seatLockRepository.findBySessionId(sessionId);
        locks.forEach(lock -> {
            if (lock.getStatus() == SeatLockStatus.LOCKED) {
                lock.setStatus(SeatLockStatus.RELEASED);
            }
        });
        seatLockRepository.saveAll(locks);
    }

    /**
     * Confirm locks for a session (after successful payment)
     */
    @Transactional
    public void confirmLocks(String sessionId) {
        List<SeatLock> locks = seatLockRepository.findBySessionId(sessionId);
        locks.forEach(lock -> {
            if (lock.getStatus() == SeatLockStatus.LOCKED) {
                lock.setStatus(SeatLockStatus.CONFIRMED);
            }
        });
        seatLockRepository.saveAll(locks);
    }

    /**
     * Get remaining time for locks in a session (in seconds)
     */
    public Long getRemainingTime(String sessionId) {
        List<SeatLock> locks = seatLockRepository.findBySessionId(sessionId);
        if (locks.isEmpty()) {
            return 0L;
        }

        SeatLock firstLock = locks.get(0);
        if (firstLock.getStatus() != SeatLockStatus.LOCKED) {
            return 0L;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = firstLock.getExpiryTime();
        
        if (now.isAfter(expiryTime)) {
            return 0L;
        }

        return java.time.Duration.between(now, expiryTime).getSeconds();
    }

    /**
     * Get list of locked seats for a show
     */
    public List<String> getLockedSeatsForShow(Integer showId) {
        LocalDateTime now = LocalDateTime.now();
        List<SeatLock> activeLocks = seatLockRepository.findActiveLocksForShow(showId, now);
        return activeLocks.stream()
                .map(SeatLock::getSeatNumber)
                .collect(Collectors.toList());
    }

    /**
     * Check if specific seats are available
     */
    public Map<String, Boolean> checkSeatsAvailability(Integer showId, List<String> seatNumbers) {
        releaseExpiredLocks();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Boolean> availability = new HashMap<>();

        for (String seatNumber : seatNumbers) {
            Optional<SeatLock> lock = seatLockRepository.findActiveLock(showId, seatNumber, now);
            availability.put(seatNumber, lock.isEmpty());
        }

        return availability;
    }

    /**
     * Scheduled task to release expired locks
     * Runs every 2 minutes
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    @Transactional
    public void releaseExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        int releasedCount = seatLockRepository.releaseExpiredLocks(now);
        if (releasedCount > 0) {
            System.out.println("Released " + releasedCount + " expired seat locks");
        }
    }

    /**
     * Extend lock time for a session (if user needs more time)
     */
    @Transactional
    public void extendLockTime(String sessionId, int additionalMinutes) throws Exception {
        List<SeatLock> locks = seatLockRepository.findBySessionId(sessionId);
        
        if (locks.isEmpty()) {
            throw new Exception("No locks found for session");
        }

        LocalDateTime now = LocalDateTime.now();
        for (SeatLock lock : locks) {
            if (lock.getStatus() == SeatLockStatus.LOCKED && lock.getExpiryTime().isAfter(now)) {
                lock.setExpiryTime(lock.getExpiryTime().plusMinutes(additionalMinutes));
            }
        }

        seatLockRepository.saveAll(locks);
    }

    /**
     * Get locked seats for a session
     */
    public List<String> getLockedSeats(String sessionId) {
        List<SeatLock> locks = seatLockRepository.findBySessionId(sessionId);
        return locks.stream()
                .filter(lock -> lock.getStatus() == SeatLockStatus.LOCKED || 
                              lock.getStatus() == SeatLockStatus.CONFIRMED)
                .map(SeatLock::getSeatNumber)
                .collect(Collectors.toList());
    }

    /**
     * Get show from session
     */
    public Show getShowFromSession(String sessionId) {
        List<SeatLock> locks = seatLockRepository.findBySessionId(sessionId);
        if (!locks.isEmpty()) {
            return locks.get(0).getShow();
        }
        return null;
    }

    /**
     * Get ShowSeats for a session (for marking as booked)
     */
    public List<ShowSeat> getShowSeatsForSession(String sessionId) {
        List<SeatLock> locks = seatLockRepository.findBySessionId(sessionId);
        if (locks.isEmpty()) {
            return new ArrayList<>();
        }
        
        Show show = locks.get(0).getShow();
        List<String> seatNumbers = locks.stream()
            .map(SeatLock::getSeatNumber)
            .collect(Collectors.toList());
        
        // Find matching show seats
        return show.getShowSeatList().stream()
            .filter(showSeat -> {
                String seatNum = showSeat.getSeatNo();
                return seatNumbers.contains(seatNum);
            })
            .collect(Collectors.toList());
    }
}
