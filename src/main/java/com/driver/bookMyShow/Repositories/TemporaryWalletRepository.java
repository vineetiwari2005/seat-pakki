package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.TemporaryWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TemporaryWalletRepository extends JpaRepository<TemporaryWallet, Long> {
    
    @Query("SELECT tw FROM TemporaryWallet tw WHERE tw.userId = :userId AND tw.isExpired = false AND tw.isUsed = false AND tw.expiresAt > :now ORDER BY tw.expiresAt ASC")
    List<TemporaryWallet> findActiveByUserIdOrderByExpiresAtAsc(@Param("userId") Integer userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT tw FROM TemporaryWallet tw WHERE tw.userId = :userId AND tw.isExpired = false AND tw.isUsed = false")
    List<TemporaryWallet> findActiveRewardsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COALESCE(SUM(tw.amount), 0) FROM TemporaryWallet tw WHERE tw.userId = :userId AND tw.isExpired = false AND tw.isUsed = false")
    Double getTotalActiveRewardAmount(@Param("userId") Long userId);
    
    @Query("SELECT tw FROM TemporaryWallet tw WHERE tw.isExpired = false AND tw.expiresAt < CURRENT_TIMESTAMP")
    List<TemporaryWallet> findExpiredButNotMarkedRewards();
    
    @Query("SELECT tw FROM TemporaryWallet tw WHERE tw.userId = :userId ORDER BY tw.createdAt DESC")
    List<TemporaryWallet> findByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);
    
    @Query("SELECT tw FROM TemporaryWallet tw WHERE tw.userId = :userId AND tw.isExpired = false AND tw.isUsed = false ORDER BY tw.expiresAt ASC")
    List<TemporaryWallet> findByUserIdAndIsExpiredFalseAndIsUsedFalseOrderByExpiresAtAsc(@Param("userId") Integer userId);
}
