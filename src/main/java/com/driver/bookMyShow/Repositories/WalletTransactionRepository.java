package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Enums.TransactionType;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Models.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * WalletTransactionRepository - Data access for wallet transactions
 * 
 * Design:
 * - Query methods follow Spring Data naming conventions
 * - Pageable support for large transaction histories
 * - No hardcoded queries - use method naming
 * - Indexed queries for performance
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Integer> {

    /**
     * Find all transactions for a user, ordered by timestamp descending
     * Latest transactions first
     */
    List<WalletTransaction> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find paginated transactions for a user
     */
    Page<WalletTransaction> findByUser(User user, Pageable pageable);

    /**
     * Find transactions by type for a user
     */
    List<WalletTransaction> findByUserAndTransactionTypeOrderByCreatedAtDesc(
        User user, 
        TransactionType transactionType
    );

    /**
     * Find transaction by reference (idempotency check)
     */
    Optional<WalletTransaction> findByTransactionReference(String transactionReference);

    /**
     * Find transactions within date range
     */
    List<WalletTransaction> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(
        User user,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    /**
     * Count transactions for a user
     */
    long countByUser(User user);

    /**
     * Sum of all credits for a user
     */
    @Query("SELECT COALESCE(SUM(wt.amount), 0.0) FROM WalletTransaction wt " +
           "WHERE wt.user = :user AND wt.transactionType = 'CREDIT'")
    Double sumCreditsByUser(@Param("user") User user);

    /**
     * Sum of all debits for a user
     */
    @Query("SELECT COALESCE(SUM(wt.amount), 0.0) FROM WalletTransaction wt " +
           "WHERE wt.user = :user AND wt.transactionType = 'DEBIT'")
    Double sumDebitsByUser(@Param("user") User user);
}
