package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.GameRewardCredit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameRewardCreditRepository extends JpaRepository<GameRewardCredit, Integer> {

    @Query("SELECT g FROM GameRewardCredit g WHERE g.user.id = :userId AND g.isActive = true AND g.expiresAt > :now AND g.remainingAmount > 0 ORDER BY g.expiresAt ASC, g.createdAt ASC")
    List<GameRewardCredit> findActiveCredits(@Param("userId") Integer userId, @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GameRewardCredit g WHERE g.user.id = :userId AND g.isActive = true AND g.expiresAt > :now AND g.remainingAmount > 0 ORDER BY g.expiresAt ASC, g.createdAt ASC")
    List<GameRewardCredit> findActiveCreditsForUpdate(@Param("userId") Integer userId, @Param("now") LocalDateTime now);

    List<GameRewardCredit> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
