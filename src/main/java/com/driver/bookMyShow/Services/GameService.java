package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Dtos.RequestDtos.GameScoreSubmissionDto;
import com.driver.bookMyShow.Dtos.ResponseDtos.GameRewardResponseDto;
import com.driver.bookMyShow.Models.*;
import com.driver.bookMyShow.Repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@RequiredArgsConstructor
@Transactional
public class GameService {
    
    private final DailyGameLogRepository dailyGameLogRepository;
    private final TemporaryWalletRepository temporaryWalletRepository;
    private final GameLeaderboardRepository gameLeaderboardRepository;
    private final MonthlyGameStatsRepository monthlyGameStatsRepository;
    private final SpinTransactionRepository spinTransactionRepository;
    private final SpinPaymentRepository spinPaymentRepository;
    private final UserExtraSpinsRepository userExtraSpinsRepository;
    private final TemporaryWalletUsageRepository temporaryWalletUsageRepository;
    private final UserRepository userRepository;
    private final UserWalletService userWalletService;
    
    private static final Random RANDOM = new Random();
    private static final double BAD_LUCK_PROBABILITY = 0.30; // 30% chance of no reward
    private static final BigDecimal MAX_REWARD = new BigDecimal("20");
    private static final BigDecimal HIGH_REWARD_MIN = new BigDecimal("10");
    private static final BigDecimal HIGH_REWARD_MAX = new BigDecimal("12");
    private static final BigDecimal MED_REWARD_MIN = new BigDecimal("7");
    private static final BigDecimal MED_REWARD_MAX = new BigDecimal("8");
    private static final BigDecimal LOW_REWARD_MIN = new BigDecimal("3");
    private static final BigDecimal LOW_REWARD_MAX = new BigDecimal("4");
    
    // Daily Spinning Wheel Constants
    private static final BigDecimal EXTRA_SPIN_PRICE = new BigDecimal("10");
    private static final int SPIN_REWARD_VALIDITY_DAYS = 10;
    
    /**
     * Main method to calculate and award cashback for game score
     */
    public GameRewardResponseDto submitGameScore(GameScoreSubmissionDto request) {
        Long userId = request.getUserId();
        Integer userScore = request.getScore();
        LocalDate today = LocalDate.now();
        
        // 1. Check if user already played today
        if (dailyGameLogRepository.hasPlayedToday(userId, today)) {
            throw new RuntimeException("You have already played your daily game. Come back tomorrow!");
        }
        
        // 2. Get current month in YYYY-MM format
        String currentMonth = YearMonth.now().toString();
        
        // 3. Fetch global stats for the current month
        Integer highestScoreOfMonth = gameLeaderboardRepository.getHighestScoreOfMonth(currentMonth);
        Double averageScoreObj = gameLeaderboardRepository.getAverageScoreOfMonth(currentMonth);
        
        // Handle null case for first month
        if (highestScoreOfMonth == null) {
            highestScoreOfMonth = 0;
        }
        if (averageScoreObj == null) {
            averageScoreObj = 0.0;
        }
        
        Integer averageScore = averageScoreObj.intValue();
        
        // 4. Log the user's score for today
        DailyGameLog gameLog = new DailyGameLog();
        gameLog.setUserId(userId);
        gameLog.setScore(userScore);
        gameLog.setPlayedDate(today);
        dailyGameLogRepository.save(gameLog);
        
        // 5. Update or create game leaderboard entry
        Optional<GameLeaderboard> existingEntry = gameLeaderboardRepository.findByUserIdAndMonthYear(userId, currentMonth);
        
        GameLeaderboard leaderboardEntry;
        if (existingEntry.isPresent()) {
            leaderboardEntry = existingEntry.get();
            if (userScore > leaderboardEntry.getHighestScore()) {
                leaderboardEntry.setHighestScore(userScore);
            }
            leaderboardEntry.setTotalPlays(leaderboardEntry.getTotalPlays() + 1);
            
            // Recalculate average
            int totalScoreForMonth = leaderboardEntry.getHighestScore(); // Simplified: using highest
            BigDecimal avg = BigDecimal.valueOf((double) totalScoreForMonth / leaderboardEntry.getTotalPlays());
            leaderboardEntry.setAverageScore(avg);
        } else {
            leaderboardEntry = new GameLeaderboard();
            leaderboardEntry.setUserId(userId);
            leaderboardEntry.setMonthYear(currentMonth);
            leaderboardEntry.setHighestScore(userScore);
            leaderboardEntry.setTotalPlays(1);
            leaderboardEntry.setAverageScore(BigDecimal.valueOf(userScore));
        }
        
        gameLeaderboardRepository.save(leaderboardEntry);
        
        // 6. Apply the "Addiction" Factor - 30% chance of NO reward regardless of score
        if (RANDOM.nextDouble() < BAD_LUCK_PROBABILITY) {
            return new GameRewardResponseDto(
                BigDecimal.ZERO,
                "Better luck next time! No reward today.",
                false
            );
        }
        
        // 7. Check if user's score meets minimum threshold
        if (userScore < averageScore) {
            return new GameRewardResponseDto(
                BigDecimal.ZERO,
                "Good try! Your score is below average. Come back tomorrow.",
                false
            );
        }
        
        // 8. Calculate reward based on performance tiers
        BigDecimal rewardAmount = calculateReward(userScore, highestScoreOfMonth, averageScore);
        
        // 9. Save to Temporary Wallet
        if (rewardAmount.compareTo(BigDecimal.ZERO) > 0) {
            TemporaryWallet wallet = new TemporaryWallet();
            wallet.setUserId(userId);
            wallet.setAmount(rewardAmount);
            wallet.setEarnedAt(LocalDateTime.now());
            // Note: expiresAt will be set to (earnedAt + 15 days) by @PrePersist
            // DO NOT manually set expiresAt here - let @PrePersist handle it
            wallet.setIsExpired(false);
            wallet.setIsUsed(false);
            
            TemporaryWallet savedWallet = temporaryWalletRepository.save(wallet);
            
            return new GameRewardResponseDto(
                rewardAmount,
                "🎉 Lucky you! Your reward has been credited. It expires in 15 days.",
                true
            );
        }
        
        return new GameRewardResponseDto(
            BigDecimal.ZERO,
            "Good try! Come back tomorrow.",
            false
        );
    }
    
    /**
     * Calculate reward amount based on score tiers
     */
    private BigDecimal calculateReward(Integer userScore, Integer highestScoreOfMonth, Integer averageScore) {
        // If score >= highest score of month: Award max (₹20 base, capped)
        if (userScore >= highestScoreOfMonth && highestScoreOfMonth > 0) {
            return getRandomBigDecimal(HIGH_REWARD_MIN, HIGH_REWARD_MAX);
        }
        
        // If score > average score * 1.5: Award ₹10-12 (very high score)
        if (userScore > (averageScore * 1.5)) {
            return getRandomBigDecimal(HIGH_REWARD_MIN, HIGH_REWARD_MAX);
        }
        
        // If score > average score: Award ₹7-8 (high score)
        if (userScore > averageScore) {
            return getRandomBigDecimal(MED_REWARD_MIN, MED_REWARD_MAX);
        }
        
        // If score >= average score: Award ₹3-4 (decent score)
        if (userScore >= averageScore) {
            return getRandomBigDecimal(LOW_REWARD_MIN, LOW_REWARD_MAX);
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Generate a random BigDecimal between min and max (inclusive)
     */
    private BigDecimal getRandomBigDecimal(BigDecimal min, BigDecimal max) {
        long minLong = min.multiply(BigDecimal.valueOf(100)).longValue();
        long maxLong = max.multiply(BigDecimal.valueOf(100)).longValue();
        long randomValue = minLong + (long) (RANDOM.nextDouble() * (maxLong - minLong + 1));
        return BigDecimal.valueOf(randomValue).divide(BigDecimal.valueOf(100));
    }
    
    /**
     * Get user's active rewards from temporary wallet
     */
    public List<TemporaryWallet> getUserActiveRewards(Long userId) {
        return temporaryWalletRepository.findActiveRewardsByUserId(userId);
    }
    
    /**
     * Get total active reward amount for user
     */
    public Double getUserTotalActiveRewardAmount(Long userId) {
        return temporaryWalletRepository.getTotalActiveRewardAmount(userId);
    }
    
    /**
     * Mark expired rewards
     */
    public void markExpiredRewards() {
        List<TemporaryWallet> expiredRewards = temporaryWalletRepository.findExpiredButNotMarkedRewards();
        expiredRewards.forEach(wallet -> wallet.setIsExpired(true));
        temporaryWalletRepository.saveAll(expiredRewards);
    }
    
    /**
     * Use a reward from temporary wallet
     */
    public void useReward(Long rewardId) {
        Optional<TemporaryWallet> wallet = temporaryWalletRepository.findById(rewardId);
        if (wallet.isPresent()) {
            TemporaryWallet tw = wallet.get();
            tw.setIsUsed(true);
            tw.setUsedAt(LocalDateTime.now());
            temporaryWalletRepository.save(tw);
        }
    }
    
    /**
     * Scheduled task to reward the top scorer of the previous month with ₹400
     * Runs at 00:05 AM on the 1st day of every month
     */
    @Scheduled(cron = "0 5 0 1 * *") // 00:05 AM on 1st day of every month
    public void awardMonthlyWinner() {
        try {
            // Get previous month in YYYY-MM format
            YearMonth previousMonth = YearMonth.now().minusMonths(1);
            String previousMonthStr = previousMonth.toString();
            
            // Find top scorer of previous month
            List<GameLeaderboard> topScorers = gameLeaderboardRepository.findTopScoresByMonth(previousMonthStr);
            
            if (topScorers.isEmpty()) {
                System.out.println("No players found for month: " + previousMonthStr);
                return;
            }
            
            // Get the top scorer (first in the list since it's ordered DESC)
            GameLeaderboard topScorer = topScorers.get(0);
            Long winnerId = topScorer.getUserId();
            
            // Award ₹400 to the monthly winner
            TemporaryWallet monthlyReward = new TemporaryWallet();
            monthlyReward.setUserId(winnerId);
            monthlyReward.setAmount(new BigDecimal("400"));
            monthlyReward.setEarnedAt(LocalDateTime.now());
            monthlyReward.setExpiresAt(LocalDateTime.now().plusDays(30)); // 30 days expiry for monthly reward
            monthlyReward.setIsExpired(false);
            monthlyReward.setIsUsed(false);
            
            TemporaryWallet savedReward = temporaryWalletRepository.save(monthlyReward);
            
            System.out.println("✅ Monthly winner awarded! User ID: " + winnerId + " received ₹400");
        } catch (Exception e) {
            System.err.println("Error awarding monthly winner: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== DAILY SPINNING WHEEL METHODS ====================
    
    /**
     * Check daily spin status for a user
     * Returns: hasSpunToday, extraSpinsBalance, timeUntilNextSpin
     */
    public SpinStatusData getSpinStatus(Integer userId) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

        // Check if user has used free spin in last 24 hours
        boolean hasSpunToday = spinTransactionRepository.hasUsedFreeSpinSince(userId, cutoffTime);
        
        // Get extra spins balance
        UserExtraSpins extraSpins = userExtraSpinsRepository.findByUserId(userId)
            .orElseGet(() -> UserExtraSpins.builder()
                .userId(userId)
                .balance(0)
                .build());
        
        // Calculate time until next spin
        String timeUntilNextSpin = "";
        if (hasSpunToday) {
            timeUntilNextSpin = calculateTimeUntilNextSpin(userId);
        }
        
        return SpinStatusData.builder()
            .hasSpunToday(hasSpunToday)
            .extraSpinsBalance(extraSpins.getBalance())
            .timeUntilNextSpin(timeUntilNextSpin)
            .build();
    }
    
    /**
     * Calculate time remaining until next spin (24 hours after last spin)
     */
    public String calculateTimeUntilNextSpin(Integer userId) {
        Optional<SpinTransaction> lastFreeSpin = spinTransactionRepository.findTopByUserIdAndUsedExtraSpinFalseOrderByCreatedAtDesc(userId);
        
        if (lastFreeSpin.isEmpty()) {
            return "00:00:00";
        }

        LocalDateTime nextSpinTime = lastFreeSpin.get().getCreatedAt().plusHours(24);
        LocalDateTime now = LocalDateTime.now();

        Long secondsRemaining = java.time.temporal.ChronoUnit.SECONDS.between(now, nextSpinTime);
        if (secondsRemaining <= 0) {
            return "00:00:00";
        }
        
        long hours = secondsRemaining / 3600;
        long minutes = (secondsRemaining % 3600) / 60;
        long seconds = secondsRemaining % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    /**
     * Get time remaining until next spin in seconds
     */
    public Long getTimeUntilNextSpinSeconds(Integer userId) {
        Optional<SpinTransaction> lastFreeSpin = spinTransactionRepository.findTopByUserIdAndUsedExtraSpinFalseOrderByCreatedAtDesc(userId);
        
        if (lastFreeSpin.isEmpty()) {
            return 0L;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextSpinTime = lastFreeSpin.get().getCreatedAt().plusHours(24);
        
        Long secondsRemaining = java.time.temporal.ChronoUnit.SECONDS.between(now, nextSpinTime);
        return Math.max(secondsRemaining, 0L);
    }
    
    /**
     * Generate spin reward from server-side weighted probability.
     * Distribution (1..10000):
     * - ₹1: 5000 (50%)
     * - ₹2: 3000 (30%)
     * - ₹3: 1000 (10%)
     * - ₹5: 500 (5%)
     * - ₹10: 300 (3%)
     * - ₹20: 100 (1%)
     * - ₹50: 60 (0.6%)
     * - ₹100: 30 (0.3%)
     * - ₹200: 7 (0.07%)
     * - ₹300: 2 (0.02%)
     * - ₹500: 1 (0.01%) ≈ once in 10,000 spins
     */
    private BigDecimal generateSpinRewardByProbability() {
        int roll = RANDOM.nextInt(10_000) + 1;

        if (roll <= 5000) return new BigDecimal("1");
        if (roll <= 8000) return new BigDecimal("2");
        if (roll <= 9000) return new BigDecimal("3");
        if (roll <= 9500) return new BigDecimal("5");
        if (roll <= 9800) return new BigDecimal("10");
        if (roll <= 9900) return new BigDecimal("20");
        if (roll <= 9960) return new BigDecimal("50");
        if (roll <= 9990) return new BigDecimal("100");
        if (roll <= 9997) return new BigDecimal("200");
        if (roll <= 9999) return new BigDecimal("300");
        return new BigDecimal("500");
    }

    /**
     * Record a spin with server-generated reward.
     * Flow: frontend -> controller -> service -> repository -> db
     */
    public com.driver.bookMyShow.Dtos.ResponseDtos.SpinRecordResponseDto recordSpin(Integer userId, Boolean usedExtraSpin) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        boolean usingExtraSpin = usedExtraSpin != null && usedExtraSpin;

        if (!usingExtraSpin) {
            LocalDateTime cutoffTime = now.minusHours(24);
            if (spinTransactionRepository.hasUsedFreeSpinSince(userId, cutoffTime)) {
                throw new RuntimeException("Free spin already used. Please wait for cooldown or use extra spin.");
            }
        }

        UserExtraSpins extraSpins = userExtraSpinsRepository.findByUserId(userId)
            .orElseGet(() -> UserExtraSpins.builder()
                .userId(userId)
                .balance(0)
                .build());

        if (usingExtraSpin && extraSpins.getBalance() <= 0) {
            throw new RuntimeException("No extra spins available");
        }

        BigDecimal rewardAmount = generateSpinRewardByProbability();
        
        // Create spin transaction
        SpinTransaction spinTx = SpinTransaction.builder()
            .userId(userId)
            .rewardAmount(rewardAmount)
            .spinDate(today)
            .usedExtraSpin(usingExtraSpin)
            .build();
        
        SpinTransaction savedTx = spinTransactionRepository.save(spinTx);
        
        // If reward is > 0, add to temporary wallet
        Long walletId = null;
        if (rewardAmount.compareTo(BigDecimal.ZERO) > 0) {
            TemporaryWallet wallet = new TemporaryWallet();
            wallet.setUserId(userId.longValue());
            wallet.setAmount(rewardAmount);
            wallet.setSourceType("GAME_REWARD"); // Track that this came from a game spin
            wallet.setEarnedAt(LocalDateTime.now());
            wallet.setExpiresAt(LocalDateTime.now().plusDays(SPIN_REWARD_VALIDITY_DAYS));
            wallet.setIsExpired(false);
            wallet.setIsUsed(false);
            
            TemporaryWallet savedWallet = temporaryWalletRepository.save(wallet);
            walletId = savedWallet.getId();
        }
        
        // Update extra spins if used
        Integer remainingExtraSpins = 0;
        if (usingExtraSpin) {
            if (extraSpins.getBalance() > 0) {
                extraSpins.setBalance(extraSpins.getBalance() - 1);
                userExtraSpinsRepository.save(extraSpins);
            }
        }
        remainingExtraSpins = extraSpins.getBalance();
        
        return com.driver.bookMyShow.Dtos.ResponseDtos.SpinRecordResponseDto.builder()
            .transactionId(savedTx.getTransactionId())
            .rewardAmount(rewardAmount)
            .walletId(walletId)
            .remainingExtraSpins(remainingExtraSpins)
            .success(true)
            .message("Spin recorded successfully")
            .build();
    }
    
    /**
     * Get extra spins balance for a user
     */
    public Integer getExtraSpinsBalance(Integer userId) {
        return userExtraSpinsRepository.findByUserId(userId)
            .map(UserExtraSpins::getBalance)
            .orElse(0);
    }
    
    /**
     * Process payment for extra spin purchase
     * Handles CARD, MAIN_WALLET, and TEMPORARY_WALLET payment methods (SEPARATE)
     * For TEMPORARY_WALLET: Uses greedy deduction (expire soonest first)
     */
    public com.driver.bookMyShow.Dtos.ResponseDtos.SpinPaymentResponseDto purchaseExtraSpin(Integer userId, BigDecimal amount, String paymentMethod) {
        try {
            String spinPaymentReference = "SPIN_PAY_" + System.currentTimeMillis() + "_" + userId;
            com.driver.bookMyShow.Enums.PaymentMethod method;
            
            if ("CARD".equalsIgnoreCase(paymentMethod)) {
                method = com.driver.bookMyShow.Enums.PaymentMethod.CREDIT_CARD;
                // No deduction needed for card payment - handled by Stripe
            } 
            else if ("MAIN_WALLET".equalsIgnoreCase(paymentMethod)) {
                method = com.driver.bookMyShow.Enums.PaymentMethod.WALLET;
                // Deduct directly from MAIN wallet (no expiry)
                try {
                    userWalletService.debitWallet(userId, amount.doubleValue(), spinPaymentReference, 
                                                 "Extra Spin Purchase (Main Wallet)");
                } catch (Exception e) {
                    throw new RuntimeException("Main wallet deduction failed: " + e.getMessage());
                }
            } 
            else if ("TEMPORARY_WALLET".equalsIgnoreCase(paymentMethod)) {
                method = com.driver.bookMyShow.Enums.PaymentMethod.WALLET;
                // Deduct from TEMPORARY wallet using GREEDY algorithm (expire soonest first)
                try {
                    deductTemporaryWalletOnlyGreedy(userId, amount, spinPaymentReference);
                } catch (Exception e) {
                    throw new RuntimeException("Temporary wallet deduction failed: " + e.getMessage());
                }
            } 
            else {
                throw new RuntimeException("Unsupported payment method: " + paymentMethod);
            }
            
            // Create payment record
            SpinPayment payment = SpinPayment.builder()
                .userId(userId)
                .amount(amount)
                .paymentMethod(method)
                .paymentStatus("COMPLETED")
                .transactionId(spinPaymentReference)
                .extraSpinsPurchased(1)
                .build();
            
            SpinPayment savedPayment = spinPaymentRepository.save(payment);
            
            // Update or create extra spins balance
            UserExtraSpins extraSpins = userExtraSpinsRepository.findByUserId(userId)
                .orElseGet(() -> UserExtraSpins.builder()
                    .userId(userId)
                    .balance(0)
                    .build());
            
            extraSpins.setBalance(extraSpins.getBalance() + 1);
            userExtraSpinsRepository.save(extraSpins);
            
            return com.driver.bookMyShow.Dtos.ResponseDtos.SpinPaymentResponseDto.builder()
                .transactionId(savedPayment.getTransactionId())
                .paymentAmount(amount)
                .extraSpinsPurchased(1)
                .newBalance(extraSpins.getBalance())
                .success(true)
                .message("Extra spin purchased successfully via " + paymentMethod)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }
    }

    /**
     * Deduct ONLY from temporary wallet in greedy order (earliest expiry first)
     * and persist outgoing usage rows for history.
     */
    @Transactional
    public void deductTemporaryWalletOnlyGreedy(Integer userId, BigDecimal amount, String paymentTransactionId) {
        List<TemporaryWallet> activeFunds = temporaryWalletRepository
            .findActiveByUserIdOrderByExpiresAtAsc(userId, LocalDateTime.now());

        BigDecimal totalAvailable = activeFunds.stream()
            .map(TemporaryWallet::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAvailable.compareTo(amount) < 0) {
            throw new RuntimeException(
                "Insufficient temporary wallet balance. Available: ₹" + totalAvailable + ", Required: ₹" + amount
            );
        }

        BigDecimal remainingAmount = amount;
        for (TemporaryWallet fund : activeFunds) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal availableInFund = fund.getAmount();
            BigDecimal deduction = remainingAmount.min(availableInFund);

            if (deduction.compareTo(availableInFund) == 0) {
                fund.setAmount(BigDecimal.ZERO);
                fund.setIsUsed(true);
                fund.setUsedAt(LocalDateTime.now());
            } else {
                fund.setAmount(availableInFund.subtract(deduction));
            }

            temporaryWalletRepository.save(fund);

            temporaryWalletUsageRepository.save(TemporaryWalletUsage.builder()
                .userId(userId.longValue())
                .temporaryWalletId(fund.getId())
                .paymentTransactionId(paymentTransactionId)
                .amount(deduction)
                .sourceType("TEMP_WALLET_SPIN_DEBIT")
                .build());

            remainingAmount = remainingAmount.subtract(deduction);
        }
    }
    
    /**
     * Deduct funds using greedy algorithm: prioritize expiring temporary funds first
     * This ensures funds closest to expiration are used first for better UX
     * 
     * @param userId User ID
     * @param amount Amount to deduct in BigDecimal
     * @param transactionRef Transaction reference for audit
     * @param description Transaction description
     * @throws Exception if insufficient balance
     */
    @Transactional
    public void deductInPriorityOrder(Integer userId, BigDecimal amount, 
                                     String transactionRef, String description) throws Exception {
        // Step 1: Fetch all active temporary wallet funds, sorted by expiration date (ascending)
        // This ensures soonest-to-expire funds are deducted first (greedy algorithm)
        List<TemporaryWallet> activeFunds = temporaryWalletRepository
            .findByUserIdAndIsExpiredFalseAndIsUsedFalseOrderByExpiresAtAsc(userId);
        
        BigDecimal remainingAmount = amount;
        BigDecimal tempFundsUsed = BigDecimal.ZERO;
        
        // Step 2: Deduct from temporary funds in priority order (expiration ascending)
        for (TemporaryWallet fund : activeFunds) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            BigDecimal fundAmount = fund.getAmount();
            BigDecimal deductionAmount = remainingAmount.min(fundAmount);
            
            // Mark fund as used if fully consumed
            if (deductionAmount.compareTo(fundAmount) == 0) {
                fund.setIsUsed(true);
                fund.setUsedAt(LocalDateTime.now());
            } else {
                // Partially used - reduce the amount
                fund.setAmount(fundAmount.subtract(deductionAmount));
            }
            
            temporaryWalletRepository.save(fund);
            tempFundsUsed = tempFundsUsed.add(deductionAmount);
            remainingAmount = remainingAmount.subtract(deductionAmount);
        }
        
        // Step 3: If amount remaining, deduct from main wallet balance
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            Double remainingDouble = remainingAmount.doubleValue();
            userWalletService.debitWallet(userId, remainingDouble, transactionRef, description);
        }
    }
    
    /**
     * Auto-expiry scheduler: Mark temporary funds as expired after 15 days
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void markExpiredTemporaryFunds() {
        LocalDateTime expiryThreshold = LocalDateTime.now();
        List<TemporaryWallet> expiredFunds = temporaryWalletRepository
            .findExpiredButNotMarkedRewards();
        
        for (TemporaryWallet fund : expiredFunds) {
            if (fund.getExpiresAt().isBefore(expiryThreshold)) {
                fund.setIsExpired(true);
            }
        }
        
        temporaryWalletRepository.saveAll(expiredFunds);
        if (!expiredFunds.isEmpty()) {
            org.slf4j.LoggerFactory.getLogger(this.getClass())
                .info("Auto-expiry: Marked {} temporary funds as expired", expiredFunds.size());
        }
    }
    
    /**
     * Get all wallet transactions for a user (spin earnings)
     */
    public List<TemporaryWallet> getUserWalletTransactions(Integer userId) {
        return temporaryWalletRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get temporary wallet transactions as DTOs for API response
     * Converts TemporaryWallet entities to API-friendly DTOs
     */
    public List<com.driver.bookMyShow.Controllers.WalletController.TemporaryWalletTransactionDTO> 
        getTemporaryWalletTransactions(Long userId) {
        // Credits (incoming) from temporary_wallet
        List<TemporaryWallet> transactions = temporaryWalletRepository.findByUserIdOrderByCreatedAtDesc(userId.intValue());
        List<com.driver.bookMyShow.Controllers.WalletController.TemporaryWalletTransactionDTO> creditEntries = transactions.stream()
            .map(tw -> com.driver.bookMyShow.Controllers.WalletController.TemporaryWalletTransactionDTO.builder()
                .id(tw.getId().intValue())
                .source(tw.getSourceType() != null ? tw.getSourceType() : "GAME_REWARD")  // Use actual source from DB
                .sourceType(tw.getSourceType() != null ? tw.getSourceType() : "GAME_REWARD")
                .amount(tw.getAmount().doubleValue())
                .expiresAt(tw.getExpiresAt())
                .isUsed(tw.getIsUsed())
                .isExpired(tw.getIsExpired())
                .usedAt(tw.getUsedAt())
                .createdAt(tw.getCreatedAt())
                .build())
            .toList();

        // Debits (outgoing) from temporary_wallet_usage
        List<TemporaryWalletUsage> debitUsages = temporaryWalletUsageRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<com.driver.bookMyShow.Controllers.WalletController.TemporaryWalletTransactionDTO> debitEntries = debitUsages.stream()
            .map(usage -> com.driver.bookMyShow.Controllers.WalletController.TemporaryWalletTransactionDTO.builder()
                .id(usage.getId().intValue())
                .source(usage.getSourceType())
                .sourceType(usage.getSourceType())
                .amount(-usage.getAmount().doubleValue())
                .expiresAt(null)
                .isUsed(true)
                .isExpired(false)
                .usedAt(usage.getCreatedAt())
                .createdAt(usage.getCreatedAt())
                .paymentTransactionId(usage.getPaymentTransactionId())
                .build())
            .toList();

        List<com.driver.bookMyShow.Controllers.WalletController.TemporaryWalletTransactionDTO> allEntries =
            new java.util.ArrayList<>(creditEntries.size() + debitEntries.size());
        allEntries.addAll(creditEntries);
        allEntries.addAll(debitEntries);
        allEntries.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return allEntries;
    }
}
