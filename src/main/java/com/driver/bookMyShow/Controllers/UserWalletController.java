package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Models.WalletTransaction;
import com.driver.bookMyShow.Models.TemporaryWalletCredit;
import com.driver.bookMyShow.Services.TemporaryWalletCreditService;
import com.driver.bookMyShow.Services.UserWalletService;
import com.driver.bookMyShow.common.dto.ApiResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserWalletController - REST API for user wallet operations
 * 
 * Design: Frontend → Controller → Service → Repository → Database
 * 
 * Endpoints:
 * GET  /api/user-wallet/{userId}/balance - Get wallet balance
 * POST /api/user-wallet/{userId}/credit  - Add money to wallet
 * POST /api/user-wallet/{userId}/debit   - Deduct money from wallet
 */
@Slf4j
@RestController
@RequestMapping("/api/user-wallet")
@CrossOrigin(origins = "*")
public class UserWalletController {

    @Autowired
    private UserWalletService userWalletService;

    @Autowired
    private TemporaryWalletCreditService temporaryWalletCreditService;

    /**
     * Get wallet balance from DB
     * GET /api/user-wallet/{userId}/balance
     */
    @GetMapping("/{userId}/balance")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> getWalletBalance(
            @PathVariable Integer userId) {
        
        try {
            log.info("[API] GET wallet balance for userId={}", userId);
            
            Double balance = userWalletService.getWalletBalance(userId);
            
            WalletBalanceResponse response = WalletBalanceResponse.builder()
                .userId(userId)
                .balance(balance)
                .build();
            
            log.info("[API] Wallet balance fetched: userId={}, balance={}", userId, balance);
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("[API] Error fetching wallet balance: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{userId}/temporary-credit")
    public ResponseEntity<ApiResponse<TemporaryCreditResponse>> getTemporaryCredit(
            @PathVariable Integer userId) {
        try {
            TemporaryWalletCreditService.TemporaryCreditResult tempCredit =
                    temporaryWalletCreditService.getAvailableCredit(userId);

            TemporaryCreditResponse response = TemporaryCreditResponse.builder()
                    .userId(userId)
                    .availableAmount(tempCredit.getAvailableAmount())
                    .expiresAt(tempCredit.getExpiresAt())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("[API] Error fetching temporary wallet credit: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{userId}/temporary-credit/transactions")
    public ResponseEntity<ApiResponse<List<TemporaryCreditTransactionResponse>>> getTemporaryCreditTransactions(
            @PathVariable Integer userId) {
        try {
            List<TemporaryWalletCredit> credits = temporaryWalletCreditService.getCreditHistory(userId);

            List<TemporaryCreditTransactionResponse> response = credits.stream()
                    .map(credit -> TemporaryCreditTransactionResponse.builder()
                            .id(credit.getId())
                            .sourceTicketId(credit.getSourceTicket() != null ? credit.getSourceTicket().getId() : null)
                            .sourceType(credit.getSourceType())
                            .totalAmount(credit.getTotalAmount())
                            .remainingAmount(credit.getRemainingAmount())
                            .expiresAt(credit.getExpiresAt())
                            .isActive(Boolean.TRUE.equals(credit.getIsActive()))
                            .lastUsedAt(credit.getLastUsedAt())
                            .createdAt(credit.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("[API] Error fetching temporary wallet transactions: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Credit wallet (add money)
     * POST /api/user-wallet/{userId}/credit
     */
    @PostMapping("/{userId}/credit")
    public ResponseEntity<ApiResponse<WalletTransactionResponse>> creditWallet(
            @PathVariable Integer userId,
            @RequestBody WalletOperationRequest request) {
        
        try {
            log.info("[API] POST credit wallet: userId={}, amount={}", userId, request.getAmount());
            
            String transactionRef = "CREDIT_" + userId + "_" + System.currentTimeMillis();
            String description = request.getDescription() != null ? 
                request.getDescription() : "Wallet recharge";
            
            WalletTransaction transaction = userWalletService.creditWallet(
                userId, 
                request.getAmount(), 
                transactionRef, 
                description
            );
            
            WalletTransactionResponse response = WalletTransactionResponse.builder()
                .transactionId(transaction.getId())
                .userId(userId)
                .amount(transaction.getAmount())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .transactionType(transaction.getTransactionType().toString())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
            
            log.info("[API] Wallet credited: userId={}, amount={}, newBalance={}", 
                    userId, request.getAmount(), transaction.getBalanceAfter());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("[API] Error crediting wallet: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Debit wallet (deduct money)
     * POST /api/user-wallet/{userId}/debit
     */
    @PostMapping("/{userId}/debit")
    public ResponseEntity<ApiResponse<WalletTransactionResponse>> debitWallet(
            @PathVariable Integer userId,
            @RequestBody WalletOperationRequest request) {
        
        try {
            log.info("[API] POST debit wallet: userId={}, amount={}", userId, request.getAmount());
            
            String transactionRef = "DEBIT_" + userId + "_" + System.currentTimeMillis();
            String description = request.getDescription() != null ? 
                request.getDescription() : "Wallet payment";
            
            WalletTransaction transaction = userWalletService.debitWallet(
                userId, 
                request.getAmount(), 
                transactionRef, 
                description
            );
            
            WalletTransactionResponse response = WalletTransactionResponse.builder()
                .transactionId(transaction.getId())
                .userId(userId)
                .amount(transaction.getAmount())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .transactionType(transaction.getTransactionType().toString())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
            
            log.info("[API] Wallet debited: userId={}, amount={}, newBalance={}", 
                    userId, request.getAmount(), transaction.getBalanceAfter());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("[API] Error debiting wallet: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== DTOs ====================

    @Data
    public static class WalletOperationRequest {
        private Double amount;
        private String description;
    }

    @Data
    @lombok.Builder
    public static class WalletBalanceResponse {
        private Integer userId;
        private Double balance;
    }

    @Data
    @lombok.Builder
    public static class WalletTransactionResponse {
        private Integer transactionId;
        private Integer userId;
        private Double amount;
        private Double balanceBefore;
        private Double balanceAfter;
        private String transactionType;
        private String description;
        private java.time.LocalDateTime createdAt;
    }

    @Data
    @lombok.Builder
    public static class TemporaryCreditResponse {
        private Integer userId;
        private Double availableAmount;
        private java.time.LocalDateTime expiresAt;
    }

    @Data
    @lombok.Builder
    public static class TemporaryCreditTransactionResponse {
        private Integer id;
        private Integer sourceTicketId;
        private String sourceType;
        private Double totalAmount;
        private Double remainingAmount;
        private java.time.LocalDateTime expiresAt;
        private Boolean isActive;
        private java.time.LocalDateTime lastUsedAt;
        private java.time.LocalDateTime createdAt;
    }
}
