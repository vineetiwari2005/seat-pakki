package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Models.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserWalletRepository - Data access layer for wallet operations
 * 
 * Features:
 * - Find wallet by user
 * - Custom update queries with optimistic locking
 * - Atomic balance updates
 */
@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Integer> {

    /**
     * Find wallet by user
     */
    Optional<UserWallet> findByUser(User user);

    /**
     * Find wallet by user ID
     */
    @Query("SELECT w FROM UserWallet w WHERE w.user.id = :userId")
    Optional<UserWallet> findByUserId(@Param("userId") Integer userId);

    /**
     * Check if wallet exists for user
     */
    boolean existsByUser(User user);

    /**
     * Delete wallet by user
     */
    void deleteByUser(User user);
}
