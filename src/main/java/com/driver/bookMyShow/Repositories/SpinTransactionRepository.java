package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.SpinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SpinTransactionRepository extends JpaRepository<SpinTransaction, Long> {
    
    /**
     * Find the spin transaction for a user on a specific date
     */
    Optional<SpinTransaction> findByUserIdAndSpinDate(Integer userId, LocalDate spinDate);
    
    /**
     * Check if user has used free spin in the last 24 hours
     */
    @Query("SELECT CASE WHEN COUNT(st) > 0 THEN true ELSE false END FROM SpinTransaction st WHERE st.userId = :userId AND st.usedExtraSpin = false AND st.createdAt >= :cutoffTime")
    boolean hasUsedFreeSpinSince(@Param("userId") Integer userId, @Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Get the last free spin for a user
     */
    Optional<SpinTransaction> findTopByUserIdAndUsedExtraSpinFalseOrderByCreatedAtDesc(Integer userId);
}
