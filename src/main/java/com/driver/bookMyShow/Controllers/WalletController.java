package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Enums.TransactionType;
import com.driver.bookMyShow.Models.WalletTransaction;
import com.driver.bookMyShow.Services.EmailService;
import com.driver.bookMyShow.Services.WalletService;
import com.driver.bookMyShow.Services.UserWalletService;
import com.driver.bookMyShow.common.dto.ApiResponse;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.UserRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WalletController - REST API for wallet operations
 * 
 * Design Principles:
 * - Controller only handles HTTP layer
 * - All business logic in WalletService
 * - Consistent API response format
 * - Proper HTTP status codes
 * 
 * Endpoints:
 * POST   /api/wallet/add-money        - Add money to wallet via card payment
 * GET    /api/wallet/balance/{userId}  - Get wallet balance
 * GET    /api/wallet/transactions/{userId} - Get transaction history
 * GET    /api/wallet/transactions/{userId}/paginated - Get paginated transactions
 */
@Slf4j
@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserWalletService userWalletService; // New DB-driven wallet service

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private com.driver.bookMyShow.Services.GameService gameService;

    /**
     * Add money to wallet via card payment
     * POST /api/wallet/add-money
     * 
     * Process:
     * 1. Validate request
     * 2. Process card payment (mock/gateway)
     * 3. Credit wallet
     * 4. Send confirmation email (async)
     */
    @PostMapping("/add-money")
    public ResponseEntity<ApiResponse<WalletAddMoneyResponse>> addMoneyToWallet(
            @RequestBody WalletAddMoneyRequest request) {
        
        try {
            log.info("Wallet top-up request: userId={}, amount={}", 
                    request.getUserId(), request.getAmount());

            // Validate request
            if (request.getAmount() == null || request.getAmount() <= 0) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Amount must be greater than zero"));
            }

            // Get user
            User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new Exception("User not found"));

            // TODO: Integrate with actual payment gateway
            // For now, mock payment success
            boolean paymentSuccess = processCardPayment(request);
            
            if (!paymentSuccess) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Payment processing failed"));
            }

            // Generate transaction reference
            String transactionRef = walletService.generateTransactionReference("WALLET_TOPUP");
            String description = "Wallet recharge via " + request.getPaymentMethod();

            // Credit wallet using new UserWalletService (DB-driven)
            WalletTransaction transaction = userWalletService.creditWallet(
                user.getId(),
                request.getAmount(),
                transactionRef,
                description
            );

            // Send email notification (async - non-blocking)
            try {
                emailService.sendWalletTopUpEmail(user, request.getAmount(), transaction.getBalanceAfter());
            } catch (Exception e) {
                log.error("Failed to send wallet top-up email: {}", e.getMessage());
                // Continue - email failure doesn't affect wallet operation
            }

            // Build response
            WalletAddMoneyResponse response = WalletAddMoneyResponse.builder()
                .transactionId(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .amount(request.getAmount())
                .newBalance(transaction.getBalanceAfter())
                .transactionDate(transaction.getCreatedAt())
                .paymentMethod(request.getPaymentMethod())
                .status("SUCCESS")
                .message("Wallet recharged successfully")
                .build();

            return ResponseEntity.ok(ApiResponse.success("Wallet recharged successfully", response));

        } catch (Exception e) {
            log.error("Error adding money to wallet: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get wallet balance (using new UserWalletService for DB-driven approach)
     * Returns both main wallet and consolidated (main + temporary) balance
     * GET /api/wallet/balance/{userId}
     */
    @GetMapping("/balance/{userId}")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> getWalletBalance(
            @PathVariable Integer userId) {
        
        try {
            // Use new UserWalletService for DB-driven wallet (main balance)
            Double mainBalance = userWalletService.getWalletBalance(userId);
            log.info("💳 Main wallet balance for user {}: ₹{}", userId, mainBalance);
            
            // Get temporary wallet balance (active rewards + refunds from booking cancellations/changes)
            Double tempBalance = gameService.getUserTotalActiveRewardAmount(userId.longValue());
            log.info("🎁 Temporary wallet balance for user {}: ₹{}", userId, tempBalance);
            
            if (tempBalance == null) {
                tempBalance = 0.0;
            }
            
            // Calculate consolidated balance
            Double consolidatedBalance = mainBalance + tempBalance;
            log.info("🔄 Consolidated balance for user {}: ₹{} (Main: ₹{} + Temp: ₹{})", 
                userId, consolidatedBalance, mainBalance, tempBalance);
            
            WalletBalanceResponse response = WalletBalanceResponse.builder()
                .userId(userId)
                .balance(mainBalance)
                .temporaryBalance(tempBalance)
                .consolidatedBalance(consolidatedBalance)
                .build();

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error fetching wallet balance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get transaction history
     * GET /api/wallet/transactions/{userId}
     */
    @GetMapping("/transactions/{userId}")
    public ResponseEntity<ApiResponse<List<WalletTransactionDTO>>> getTransactionHistory(
            @PathVariable Integer userId,
            @RequestParam(required = false) String type) {
        
        try {
            log.info("💳 Fetching transaction history for userId: {} (type filter: {})", userId, type);
            
            List<WalletTransaction> transactions;
            
            if (type != null && !type.isEmpty()) {
                TransactionType transactionType = TransactionType.valueOf(type.toUpperCase());
                transactions = walletService.getTransactionsByType(userId, transactionType);
                log.info("🔍 Filtered by type {}: {} transactions found", type, transactions.size());
            } else {
                transactions = walletService.getTransactionHistory(userId);
                log.info("📊 All transactions: {} found", transactions.size());
            }

            List<WalletTransactionDTO> response = transactions.stream()
                .map(this::toDTO)
                .toList();

            log.info("✅ Returning {} wallet transactions for user: {}", response.size(), userId);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error fetching transaction history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get temporary wallet transaction history with expiry dates
     * GET /api/wallet/temporary-transactions/{userId}
     */
    @GetMapping("/temporary-transactions/{userId}")
    public ResponseEntity<ApiResponse<List<TemporaryWalletTransactionDTO>>> getTemporaryTransactionHistory(
            @PathVariable Integer userId) {
        
        try {
            log.info("💳 Fetching temporary wallet transactions for userId: {}", userId);
            
            List<TemporaryWalletTransactionDTO> transactions = gameService.getTemporaryWalletTransactions(userId.longValue());
            
            log.info("✅ Returning {} temporary wallet transactions for user: {}", transactions.size(), userId);
            return ResponseEntity.ok(ApiResponse.success(transactions));

        } catch (Exception e) {
            log.error("Error fetching temporary transaction history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get paginated transaction history
     * GET /api/wallet/transactions/{userId}/paginated?page=0&size=10
     */
    @GetMapping("/transactions/{userId}/paginated")
    public ResponseEntity<ApiResponse<Page<WalletTransactionDTO>>> getTransactionHistoryPaginated(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Page<WalletTransaction> transactions = walletService.getTransactionHistory(userId, page, size);
            
            Page<WalletTransactionDTO> response = transactions.map(this::toDTO);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error fetching paginated transaction history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Process card payment (mock - replace with actual gateway)
     */
    private boolean processCardPayment(WalletAddMoneyRequest request) {
        // TODO: Integrate with actual payment gateway (Stripe/Razorpay)
        // For now, mock success
        log.info("Processing payment: method={}, amount={}", 
                request.getPaymentMethod(), request.getAmount());
        return true;
    }

    /**
     * Convert entity to DTO
     */
    private WalletTransactionDTO toDTO(WalletTransaction transaction) {
        double signedAmount = transaction.getAmount();
        if (transaction.getTransactionType() == TransactionType.DEBIT) {
            signedAmount = -Math.abs(transaction.getAmount());
        } else if (transaction.getTransactionType() == TransactionType.CREDIT || transaction.getTransactionType() == TransactionType.REFUND) {
            signedAmount = Math.abs(transaction.getAmount());
        }

        return WalletTransactionDTO.builder()
            .id(transaction.getId())
            .transactionType(transaction.getTransactionType().name())
            .amount(signedAmount)
            .balanceBefore(transaction.getBalanceBefore())
            .balanceAfter(transaction.getBalanceAfter())
            .transactionReference(transaction.getTransactionReference())
            .description(transaction.getDescription())
            .createdAt(transaction.getCreatedAt())
            .build();
    }

    // ==================== DTOs ====================

    @Data
    public static class WalletAddMoneyRequest {
        private Integer userId;
        private Double amount;
        private String paymentMethod; // CARD, UPI, NETBANKING
        private String cardNumber;    // Masked
        private String cardHolderName;
    }

    @Data
    @lombok.Builder
    public static class WalletAddMoneyResponse {
        private Integer transactionId;
        private String transactionReference;
        private Double amount;
        private Double newBalance;
        private LocalDateTime transactionDate;
        private String paymentMethod;
        private String status;
        private String message;
    }

    @Data
    @lombok.Builder
    public static class WalletBalanceResponse {
        private Integer userId;
        private Double balance;  // Main wallet balance
        private Double temporaryBalance;  // Active temporary wallet balance
        private Double consolidatedBalance;  // Total of both
    }

    @Data
    @lombok.Builder
    public static class WalletTransactionDTO {
        private Integer id;
        private String transactionType;
        private Double amount;
        private Double balanceBefore;
        private Double balanceAfter;
        private String transactionReference;
        private String description;
        private LocalDateTime createdAt;
    }

    @Data
    @lombok.Builder
    public static class TemporaryWalletTransactionDTO {
        private Integer id;
        private String source;  // GAME_REWARD
        private String sourceType;
        private Double amount;
        private String paymentTransactionId;
        private LocalDateTime expiresAt;
        private Boolean isUsed;
        private Boolean isExpired;
        private LocalDateTime usedAt;
        private LocalDateTime createdAt;
    }
}
