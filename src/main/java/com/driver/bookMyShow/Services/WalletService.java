package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Enums.TransactionType;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Models.WalletTransaction;
import com.driver.bookMyShow.Repositories.UserRepository;
import com.driver.bookMyShow.Repositories.WalletTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * WalletService - Handles wallet operations and transaction management
 * 
 * Design Principles:
 * - Single Responsibility: Only manages wallet balance and transactions
 * - Stateless: No instance state, all operations are self-contained
 * - Idempotent: Same operation with same reference won't duplicate
 * - Atomic: All operations are transactional
 * - Audit Trail: Complete transaction history maintained
 * 
 * System Design:
 * - Transaction-first approach (log before balance update)
 * - Retry-safe with transaction references
 * - Balance consistency maintained
 * - No distributed transactions
 */
@Slf4j
@Service
public class WalletService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;
    
    @Autowired
    private UserWalletService userWalletService; // Use DB-driven wallet service

    /**
     * Add money to wallet (CREDIT)
     * Idempotent operation using transaction reference
     * 
     * @param userId User ID
     * @param amount Amount to add
     * @param transactionReference Unique reference for idempotency
     * @param description Transaction description
     * @return WalletTransaction
     */
    @Transactional
    public WalletTransaction creditWallet(Integer userId, Double amount, 
                                          String transactionReference, String description) throws Exception {
        // Idempotency check
        Optional<WalletTransaction> existing = transactionRepository
            .findByTransactionReference(transactionReference);
        if (existing.isPresent()) {
            log.info("[EXISTING] Credit transaction already exists: {}, amount={}", transactionReference, existing.get().getAmount());
            return existing.get();
        }
        
        log.info("[NEW] Creating credit transaction: {}, amount={}", transactionReference, amount);

        if (amount <= 0) {
            throw new Exception("Amount must be greater than zero");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));

        Double balanceBefore = user.getWalletBalance();
        Double balanceAfter = balanceBefore + amount;

        // Create transaction record FIRST (audit trail)
        WalletTransaction transaction = WalletTransaction.builder()
            .user(user)
            .transactionType(TransactionType.CREDIT)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .transactionReference(transactionReference)
            .description(description)
            .build();
        
        WalletTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("[SAVED] Credit transaction saved to DB: id={}, ref={}", savedTransaction.getId(), transactionReference);

        // Update user balance AFTER transaction is logged
        user.setWalletBalance(balanceAfter);
        User savedUser = userRepository.save(user);

        log.info("[SUCCESS] Wallet credited: userId={}, amount={}, balanceBefore={}, balanceAfter={}, newBalance={}, txnId={}", 
                userId, amount, balanceBefore, balanceAfter, savedUser.getWalletBalance(), savedTransaction.getId());

        return transaction;
    }

    /**
     * Deduct money from wallet (DEBIT)
     * 
     * @param userId User ID
     * @param amount Amount to deduct
     * @param transactionReference Unique reference for idempotency
     * @param description Transaction description
     * @return WalletTransaction
     */
    @Transactional
    public WalletTransaction debitWallet(Integer userId, Double amount,
                                         String transactionReference, String description) throws Exception {
        // Idempotency check
        Optional<WalletTransaction> existing = transactionRepository
            .findByTransactionReference(transactionReference);
        if (existing.isPresent()) {
            log.info("[EXISTING] Debit transaction already exists: {}, amount={}", transactionReference, existing.get().getAmount());
            return existing.get();
        }
        
        log.info("[NEW] Creating debit transaction: {}, amount={}", transactionReference, amount);

        if (amount <= 0) {
            throw new Exception("Amount must be greater than zero");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));

        Double balanceBefore = user.getWalletBalance();
        
        if (balanceBefore < amount) {
            throw new Exception("Insufficient wallet balance. Available: ₹" + balanceBefore);
        }

        Double balanceAfter = balanceBefore - amount;

        // Create transaction record FIRST
        WalletTransaction transaction = WalletTransaction.builder()
            .user(user)
            .transactionType(TransactionType.DEBIT)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .transactionReference(transactionReference)
            .description(description)
            .build();
        
        WalletTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("[SAVED] Debit transaction saved to DB: id={}, ref={}", savedTransaction.getId(), transactionReference);

        // Update balance AFTER logging
        user.setWalletBalance(balanceAfter);
        User savedUser = userRepository.save(user);

        log.info("[SUCCESS] Wallet debited: userId={}, amount={}, balanceBefore={}, balanceAfter={}, newBalance={}, txnId={}", 
                userId, amount, balanceBefore, balanceAfter, savedUser.getWalletBalance(), savedTransaction.getId());

        return transaction;
    }

    /**
     * Process refund to wallet (REFUND)
     * 
     * @param userId User ID
     * @param amount Refund amount
     * @param transactionReference Unique reference for idempotency
     * @param description Refund description
     * @return WalletTransaction
     */
    @Transactional
    public WalletTransaction refundToWallet(Integer userId, Double amount,
                                            String transactionReference, String description) throws Exception {
        // Idempotency check
        Optional<WalletTransaction> existing = transactionRepository
            .findByTransactionReference(transactionReference);
        if (existing.isPresent()) {
            log.info("[EXISTING] Refund transaction already processed: {}, amount={}", transactionReference, existing.get().getAmount());
            return existing.get();
        }
        
        log.info("[NEW] Creating refund transaction: {}, amount={}", transactionReference, amount);

        if (amount <= 0) {
            throw new Exception("Refund amount must be greater than zero");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));

        Double balanceBefore = user.getWalletBalance();
        Double balanceAfter = balanceBefore + amount;

        // Create refund transaction record
        WalletTransaction transaction = WalletTransaction.builder()
            .user(user)
            .transactionType(TransactionType.REFUND)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .transactionReference(transactionReference)
            .description(description)
            .build();
        
        WalletTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("[SAVED] Refund transaction saved to DB: id={}, ref={}", savedTransaction.getId(), transactionReference);

        // Update balance
        user.setWalletBalance(balanceAfter);
        User savedUser = userRepository.save(user);

        log.info("[SUCCESS] Refund processed: userId={}, amount={}, balanceBefore={}, balanceAfter={}, newBalance={}, txnId={}", 
                userId, amount, balanceBefore, balanceAfter, savedUser.getWalletBalance(), savedTransaction.getId());

        return transaction;
    }

    /**
     * Get transaction history for a user
     * Latest transactions first
     */
    public List<WalletTransaction> getTransactionHistory(Integer userId) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));
        
        return transactionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get paginated transaction history
     */
    public Page<WalletTransaction> getTransactionHistory(Integer userId, int page, int size) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.findByUser(user, pageable);
    }

    /**
     * Get transactions by type
     */
    public List<WalletTransaction> getTransactionsByType(Integer userId, TransactionType type) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));
        
        return transactionRepository.findByUserAndTransactionTypeOrderByCreatedAtDesc(user, type);
    }

    /**
     * Get transactions in date range
     */
    public List<WalletTransaction> getTransactionsBetweenDates(
            Integer userId, LocalDateTime startDate, LocalDateTime endDate) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));
        
        return transactionRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(
            user, startDate, endDate);
    }

    /**
     * Get current wallet balance
     */
    public Double getWalletBalance(Integer userId) throws Exception {
        // Use DB-driven wallet service instead of user entity
        return userWalletService.getWalletBalance(userId);
    }

    /**
     * Generate unique transaction reference
     */
    public String generateTransactionReference(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
