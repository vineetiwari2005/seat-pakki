package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.TemporaryWalletCredit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TemporaryWalletCreditRepository extends JpaRepository<TemporaryWalletCredit, Integer> {

    @Query("SELECT t FROM TemporaryWalletCredit t WHERE t.user.id = :userId AND t.isActive = true AND t.expiresAt > :now AND t.remainingAmount > 0 ORDER BY t.expiresAt ASC, t.createdAt ASC")
    List<TemporaryWalletCredit> findActiveCredits(@Param("userId") Integer userId, @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TemporaryWalletCredit t WHERE t.user.id = :userId AND t.isActive = true AND t.expiresAt > :now AND t.remainingAmount > 0 ORDER BY t.expiresAt ASC, t.createdAt ASC")
    List<TemporaryWalletCredit> findActiveCreditsForUpdate(@Param("userId") Integer userId, @Param("now") LocalDateTime now);

    @Query("SELECT t FROM TemporaryWalletCredit t WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<TemporaryWalletCredit> findByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);
}