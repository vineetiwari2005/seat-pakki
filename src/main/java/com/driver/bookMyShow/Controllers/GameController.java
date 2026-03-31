package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Dtos.RequestDtos.GameScoreSubmissionDto;
import com.driver.bookMyShow.Dtos.RequestDtos.PurchaseExtraSpinRequestDto;
import com.driver.bookMyShow.Dtos.RequestDtos.RecordSpinRequestDto;
import com.driver.bookMyShow.Dtos.ResponseDtos.*;
import com.driver.bookMyShow.Models.TemporaryWallet;
import com.driver.bookMyShow.Services.GameService;
import com.driver.bookMyShow.Services.SpinStatusData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GameController {
    
    private final GameService gameService;
    
    /**
     * Submit game score and calculate reward
     * POST /api/game/submit-score
     */
    @PostMapping("/submit-score")
    public ResponseEntity<?> submitGameScore(@RequestBody GameScoreSubmissionDto request) {
        try {
            GameRewardResponseDto response = gameService.submitGameScore(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get user's active rewards
     * GET /api/game/user/{userId}/rewards
     */
    @GetMapping("/user/{userId}/rewards")
    public ResponseEntity<?> getUserActiveRewards(@PathVariable Long userId) {
        try {
            List<TemporaryWallet> rewards = gameService.getUserActiveRewards(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("rewards", rewards);
            response.put("count", rewards.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get user's total active reward amount
     * GET /api/game/user/{userId}/total-rewards
     */
    @GetMapping("/user/{userId}/total-rewards")
    public ResponseEntity<?> getUserTotalRewards(@PathVariable Long userId) {
        try {
            Double totalAmount = gameService.getUserTotalActiveRewardAmount(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("totalAmount", totalAmount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Mark expired rewards
     * POST /api/game/mark-expired
     */
    @PostMapping("/mark-expired")
    public ResponseEntity<?> markExpiredRewards() {
        try {
            gameService.markExpiredRewards();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Expired rewards marked successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Use a reward
     * POST /api/game/use-reward/{rewardId}
     */
    @PostMapping("/use-reward/{rewardId}")
    public ResponseEntity<?> useReward(@PathVariable Long rewardId) {
        try {
            gameService.useReward(rewardId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Reward used successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    // ==================== DAILY SPINNING WHEEL ENDPOINTS ====================
    
    /**
     * Get daily spin status for a user
     * GET /api/game/spin-status/{userId}
     */
    @GetMapping("/spin-status/{userId}")
    public ResponseEntity<?> getSpinStatus(@PathVariable Integer userId) {
        try {
            SpinStatusData statusData = gameService.getSpinStatus(userId);
            
            SpinStatusResponseDto response = SpinStatusResponseDto.builder()
                .hasSpunToday(statusData.getHasSpunToday())
                .extraSpinsBalance(statusData.getExtraSpinsBalance())
                .timeUntilNextSpin(statusData.getTimeUntilNextSpin())
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch spin status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get time remaining until next spin
     * GET /api/game/time-until-spin/{userId}
     */
    @GetMapping("/time-until-spin/{userId}")
    public ResponseEntity<?> getTimeUntilSpin(@PathVariable Integer userId) {
        try {
            String timeRemaining = gameService.calculateTimeUntilNextSpin(userId);
            Long secondsRemaining = gameService.getTimeUntilNextSpinSeconds(userId);
            
            TimeUntilSpinResponseDto response = TimeUntilSpinResponseDto.builder()
                .timeRemaining(timeRemaining)
                .secondsRemaining(secondsRemaining)
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to calculate time: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Record a spin result and award reward
     * POST /api/game/record-spin
     */
    @PostMapping("/record-spin")
    public ResponseEntity<?> recordSpin(@RequestBody RecordSpinRequestDto request) {
        try {
            SpinRecordResponseDto response = gameService.recordSpin(
                request.getUserId(),
                request.getUsedExtraSpin()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to record spin: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get extra spins balance for a user
     * GET /api/game/extra-spins/{userId}
     */
    @GetMapping("/extra-spins/{userId}")
    public ResponseEntity<?> getExtraSpinsBalance(@PathVariable Integer userId) {
        try {
            Integer balance = gameService.getExtraSpinsBalance(userId);
            
            ExtraSpinsBalanceResponseDto response = ExtraSpinsBalanceResponseDto.builder()
                .balance(balance)
                .totalPurchased(0)
                .totalUsed(0)
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch balance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Purchase extra spins with payment
     * POST /api/game/purchase-extra-spin
     */
    @PostMapping("/purchase-extra-spin")
    public ResponseEntity<?> purchaseExtraSpin(@RequestBody PurchaseExtraSpinRequestDto request) {
        try {
            SpinPaymentResponseDto response = gameService.purchaseExtraSpin(
                request.getUserId(),
                request.getAmount(),
                request.getPaymentMethod()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Payment failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get wallet transactions for a user
     * GET /api/game/wallet-transactions/{userId}
     */
    @GetMapping("/wallet-transactions/{userId}")
    public ResponseEntity<?> getWalletTransactions(@PathVariable Integer userId) {
        try {
            List<TemporaryWallet> transactions = gameService.getUserWalletTransactions(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("transactions", transactions);
            response.put("count", transactions.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch wallet transactions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
