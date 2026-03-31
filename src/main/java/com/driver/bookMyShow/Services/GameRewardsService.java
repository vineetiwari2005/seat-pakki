package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Models.GameMonthlyLeaderboard;
import com.driver.bookMyShow.Models.GamePlayLog;
import com.driver.bookMyShow.Models.GameRewardCredit;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.GameMonthlyLeaderboardRepository;
import com.driver.bookMyShow.Repositories.GamePlayLogRepository;
import com.driver.bookMyShow.Repositories.GameRewardCreditRepository;
import com.driver.bookMyShow.Repositories.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GameRewardsService {

    private static final int GAME_REWARD_VALIDITY_DAYS = 10;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GamePlayLogRepository gamePlayLogRepository;

    @Autowired
    private GameMonthlyLeaderboardRepository gameMonthlyLeaderboardRepository;

    @Autowired
    private GameRewardCreditRepository gameRewardCreditRepository;

    @Autowired
    private TemporaryWalletCreditService temporaryWalletCreditService;

    public GameStatusResponse getStatus(Integer userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found with id: " + userId));

        LocalDate today = LocalDate.now();
        boolean alreadyPlayedToday = gamePlayLogRepository.existsByUserIdAndPlayedDate(user.getId(), today);

        GamePlayLog todayPlay = gamePlayLogRepository.findByUserIdAndPlayedDate(user.getId(), today).orElse(null);
        GameMonthlyLeaderboard leaderboard = getCurrentMonthLeaderboard();
        TemporaryWalletCreditService.TemporaryCreditResult tempCredit = temporaryWalletCreditService.getAvailableCredit(userId);

        return GameStatusResponse.builder()
                .userId(userId)
                .canPlayToday(!alreadyPlayedToday)
                .lastPlayedDate(todayPlay == null ? null : todayPlay.getPlayedDate())
                .todayScore(todayPlay == null ? null : todayPlay.getScore())
                .currentMonth(currentMonthKey())
                .monthlyHighestScore(leaderboard.getHighestScore())
                .monthlyAverageScore(round2(leaderboard.getAverageScore()))
                .monthlyPlayCount(leaderboard.getPlaysCount())
                .temporaryWalletAvailable(round2(tempCredit.getAvailableAmount() == null ? 0.0 : tempCredit.getAvailableAmount()))
                .temporaryWalletExpiresAt(tempCredit.getExpiresAt())
                .build();
    }

    @Transactional
    public GamePlayResult submitPlay(Integer userId, Integer score, Integer moves, Integer timeTakenSeconds) throws Exception {
        if (score == null || score < 0) {
            throw new Exception("Score must be non-negative");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found with id: " + userId));

        LocalDate today = LocalDate.now();
        if (gamePlayLogRepository.existsByUserIdAndPlayedDate(userId, today)) {
            throw new Exception("You can play only once per day");
        }

        GamePlayLog playLog = gamePlayLogRepository.save(GamePlayLog.builder()
                .user(user)
                .score(score)
                .playedDate(today)
                .build());

        GameMonthlyLeaderboard leaderboard = updateMonthlyLeaderboard(score, userId);

        double rewardAmount = calculateReward(score, moves, timeTakenSeconds);
        LocalDateTime rewardExpiresAt = null;
        if (rewardAmount > 0) {
            TemporaryWalletCreditService.TemporaryCreditResult tempCreditResult =
                    temporaryWalletCreditService.createGameRewardCredit(userId, rewardAmount, GAME_REWARD_VALIDITY_DAYS);
            rewardExpiresAt = tempCreditResult.getExpiresAt();

            GameRewardCredit rewardCredit = GameRewardCredit.builder()
                    .user(user)
                    .gamePlayLogId(playLog.getId())
                    .totalAmount(rewardAmount)
                    .remainingAmount(rewardAmount)
                    .expiresAt(rewardExpiresAt)
                    .isActive(true)
                    .build();
            gameRewardCreditRepository.save(rewardCredit);
        }

        log.info("Game play recorded for user {} score {} reward {}", userId, score, rewardAmount);

        return GamePlayResult.builder()
                .playedDate(today)
                .score(score)
                .moves(moves)
                .timeTakenSeconds(timeTakenSeconds)
                .rewardAmount(round2(rewardAmount))
                .rewardExpiresAt(rewardExpiresAt)
                .rewardMessage(rewardAmount > 0
                        ? "Cashback credited to temporary wallet for 10 days."
                        : "No cashback this time. Try again tomorrow!")
                .monthlyHighestScore(leaderboard.getHighestScore())
                .monthlyAverageScore(round2(leaderboard.getAverageScore()))
                .monthlyPlayCount(leaderboard.getPlaysCount())
                .build();
    }

    public List<GameHistoryItem> getHistory(Integer userId) throws Exception {
        userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found with id: " + userId));

        return gamePlayLogRepository.findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .limit(30)
                .map(log -> GameHistoryItem.builder()
                        .playedDate(log.getPlayedDate())
                        .score(log.getScore())
                        .playedAt(log.getPlayedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private GameMonthlyLeaderboard getCurrentMonthLeaderboard() {
        String monthKey = currentMonthKey();
        return gameMonthlyLeaderboardRepository.findById(monthKey)
                .orElse(GameMonthlyLeaderboard.builder()
                        .monthKey(monthKey)
                        .highestScore(0)
                        .highestUserId(null)
                        .averageScore(0.0)
                        .playsCount(0)
                        .totalScore(0L)
                        .build());
    }

    private GameMonthlyLeaderboard updateMonthlyLeaderboard(int score, Integer userId) {
        String monthKey = currentMonthKey();
        GameMonthlyLeaderboard leaderboard = gameMonthlyLeaderboardRepository.findById(monthKey)
                .orElse(GameMonthlyLeaderboard.builder()
                        .monthKey(monthKey)
                        .highestScore(0)
                        .highestUserId(null)
                        .averageScore(0.0)
                        .playsCount(0)
                        .totalScore(0L)
                        .build());

        int nextPlays = leaderboard.getPlaysCount() + 1;
        long nextTotal = leaderboard.getTotalScore() + score;
        double nextAverage = nextPlays > 0 ? (double) nextTotal / nextPlays : 0.0;

        leaderboard.setPlaysCount(nextPlays);
        leaderboard.setTotalScore(nextTotal);
        leaderboard.setAverageScore(nextAverage);

        if (score > leaderboard.getHighestScore()) {
            leaderboard.setHighestScore(score);
            leaderboard.setHighestUserId(userId);
        }

        return gameMonthlyLeaderboardRepository.save(leaderboard);
    }

    private double calculateReward(Integer score, Integer moves, Integer timeTakenSeconds) {
        if (score == null || score <= 0) {
            return 0.0;
        }

        int safeMoves = moves == null || moves <= 0 ? 1 : moves;
        int safeTime = timeTakenSeconds == null || timeTakenSeconds <= 0 ? 1 : timeTakenSeconds;

        double efficiency = Math.max(0.5, Math.min(1.5, (100.0 / safeMoves) + (120.0 / safeTime)));
        double baseReward = Math.min(120.0, Math.max(5.0, score * 0.6 * efficiency));

        int luckRoll = ThreadLocalRandom.current().nextInt(100);
        if (luckRoll < 18) {
            return 0.0;
        }
        if (luckRoll < 40) {
            return round2(baseReward * 0.4);
        }
        if (luckRoll < 70) {
            return round2(baseReward * 0.7);
        }
        if (luckRoll < 92) {
            return round2(baseReward);
        }
        return round2(baseReward * 1.2);
    }

    private String currentMonthKey() {
        return YearMonth.now().toString();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Data
    @Builder
    public static class GameStatusResponse {
        private Integer userId;
        private Boolean canPlayToday;
        private LocalDate lastPlayedDate;
        private Integer todayScore;
        private String currentMonth;
        private Integer monthlyHighestScore;
        private Double monthlyAverageScore;
        private Integer monthlyPlayCount;
        private Double temporaryWalletAvailable;
        private LocalDateTime temporaryWalletExpiresAt;
    }

    @Data
    @Builder
    public static class GamePlayResult {
        private LocalDate playedDate;
        private Integer score;
        private Integer moves;
        private Integer timeTakenSeconds;
        private Double rewardAmount;
        private LocalDateTime rewardExpiresAt;
        private String rewardMessage;
        private Integer monthlyHighestScore;
        private Double monthlyAverageScore;
        private Integer monthlyPlayCount;
    }

    @Data
    @Builder
    public static class GameHistoryItem {
        private LocalDate playedDate;
        private Integer score;
        private LocalDateTime playedAt;
    }
}
