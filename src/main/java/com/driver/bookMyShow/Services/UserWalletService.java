package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Enums.TransactionType;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Models.UserWallet;
import com.driver.bookMyShow.Models.WalletTransaction;
import com.driver.bookMyShow.Repositories.UserRepository;
import com.driver.bookMyShow.Repositories.UserWalletRepository;
import com.driver.bookMyShow.Repositories.WalletTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * UserWalletService - DB-Driven wallet management service
 * 
 * Design Principles:
 * - Database-driven (uses UserWallet entity)
 * - Frontend → Controller → Service → Repository → Database flow
 * - Transactional operations
 * - Optimistic locking for concurrency
 * - Complete audit trail
 * 
 * Features:
 * - Get wallet balance from DB
 * - Credit wallet (add money)
 * - Debit wallet (deduct money)
 * - Create wallet for new users
 * - Fetch and update from database
 */
@Slf4j
@Service
public class UserWalletService {

    @Autowired
    private UserWalletRepository userWalletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    /**
     * Get or create wallet for user
     * 
     * @param userId User ID
     * @return UserWallet
     * @throws Exception if user not found
     */
    @Transactional
    public UserWallet getOrCreateWallet(Integer userId) throws Exception {
        log.info("[GET_OR_CREATE_WALLET] userId={}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found with id: " + userId));

        Optional<UserWallet> existingWallet = userWalletRepository.findByUser(user);
        
        if (existingWallet.isPresent()) {
            log.info("[WALLET_EXISTS] userId={}, balance={}", userId, existingWallet.get().getBalance());
            return existingWallet.get();
        }

        // Create new wallet
        UserWallet wallet = UserWallet.builder()
            .user(user)
            .balance(0.0)
            .build();
        
        UserWallet savedWallet = userWalletRepository.save(wallet);
        log.info("[WALLET_CREATED] userId={}, walletId={}, balance=0.0", userId, savedWallet.getId());
        
        return savedWallet;
    }

    /**
     * Get wallet balance from DB
     * 
     * @param userId User ID
     * @return Current wallet balance
     * @throws Exception if user not found
     */
    public Double getWalletBalance(Integer userId) throws Exception {
        log.info("[GET_BALANCE] Fetching wallet balance for userId={}", userId);
        
        UserWallet wallet = getOrCreateWallet(userId);
        Double balance = wallet.getBalance();
        
        log.info("[GET_BALANCE] userId={}, balance={}", userId, balance);
        return balance;
    }

    /**
     * Credit wallet (add money)
     * 
     * @param userId User ID
     * @param amount Amount to add
     * @param transactionReference Unique reference
     * @param description Transaction description
     * @return WalletTransaction
     * @throws Exception if operation fails
     */
    @Transactional
    public WalletTransaction creditWallet(Integer userId, Double amount, 
                                          String transactionReference, String description) throws Exception {
        log.info("[CREDIT_WALLET] userId={}, amount={}, ref={}", userId, amount, transactionReference);
        
        // Check for duplicate transaction
        Optional<WalletTransaction> existing = walletTransactionRepository
            .findByTransactionReference(transactionReference);
        if (existing.isPresent()) {
            log.info("[DUPLICATE_TXN] Transaction already exists: {}", transactionReference);
            return existing.get();
        }

        if (amount <= 0) {
            throw new Exception("Amount must be greater than zero");
        }

        // Get or create wallet
        UserWallet wallet = getOrCreateWallet(userId);
        User user = wallet.getUser();
        
        Double balanceBefore = wallet.getBalance();
        
        // Credit wallet
        wallet.credit(amount);
        Double balanceAfter = wallet.getBalance();
        
        // Save wallet to DB
        userWalletRepository.save(wallet);
        log.info("[WALLET_UPDATED] userId={}, balanceBefore={}, balanceAfter={}", 
                userId, balanceBefore, balanceAfter);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
            .user(user)
            .transactionType(TransactionType.CREDIT)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .transactionReference(transactionReference)
            .description(description)
            .build();
        
        WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);
        log.info("[TXN_CREATED] txnId={}, userId={}, type=CREDIT, amount={}", 
                savedTransaction.getId(), userId, amount);
        
        return savedTransaction;
    }

    /**
     * Debit wallet (deduct money)
     * 
     * @param userId User ID
     * @param amount Amount to deduct
     * @param transactionReference Unique reference
     * @param description Transaction description
     * @return WalletTransaction
     * @throws Exception if insufficient balance or operation fails
     */
    @Transactional
    public WalletTransaction debitWallet(Integer userId, Double amount,
                                         String transactionReference, String description) throws Exception {
        log.info("[DEBIT_WALLET] userId={}, amount={}, ref={}", userId, amount, transactionReference);
        
        // Check for duplicate transaction
        Optional<WalletTransaction> existing = walletTransactionRepository
            .findByTransactionReference(transactionReference);
        if (existing.isPresent()) {
            log.info("[DUPLICATE_TXN] Transaction already exists: {}", transactionReference);
            return existing.get();
        }

        if (amount <= 0) {
            throw new Exception("Amount must be greater than zero");
        }

        // Get or create wallet
        UserWallet wallet = getOrCreateWallet(userId);
        User user = wallet.getUser();
        
        Double balanceBefore = wallet.getBalance();
        
        // Check sufficient balance
        if (!wallet.hasSufficientBalance(amount)) {
            throw new Exception("Insufficient wallet balance. Available: ₹" + balanceBefore + 
                              ", Required: ₹" + amount);
        }
        
        // Debit wallet
        wallet.debit(amount);
        Double balanceAfter = wallet.getBalance();
        
        // Save wallet to DB
        userWalletRepository.save(wallet);
        log.info("[WALLET_UPDATED] userId={}, balanceBefore={}, balanceAfter={}", 
                userId, balanceBefore, balanceAfter);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
            .user(user)
            .transactionType(TransactionType.DEBIT)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .transactionReference(transactionReference)
            .description(description)
            .build();
        
        WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);
        log.info("[TXN_CREATED] txnId={}, userId={}, type=DEBIT, amount={}", 
                savedTransaction.getId(), userId, amount);
        
        return savedTransaction;
    }

    /**
     * Check if user has sufficient wallet balance
     * 
     * @param userId User ID
     * @param amount Required amount
     * @return true if sufficient balance, false otherwise
     */
    public boolean hasSufficientBalance(Integer userId, Double amount) {
        try {
            UserWallet wallet = getOrCreateWallet(userId);
            boolean sufficient = wallet.hasSufficientBalance(amount);
            log.info("[CHECK_BALANCE] userId={}, required={}, available={}, sufficient={}", 
                    userId, amount, wallet.getBalance(), sufficient);
            return sufficient;
        } catch (Exception e) {
            log.error("[CHECK_BALANCE_ERROR] userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }
}
