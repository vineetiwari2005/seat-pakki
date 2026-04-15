package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Services.GameRewardsService;
import com.driver.bookMyShow.common.dto.ApiResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/games/rewards")
@CrossOrigin(origins = "*")
public class GameRewardsController {

    @Autowired
    private GameRewardsService gameRewardsService;

    @GetMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<GameRewardsService.GameStatusResponse>> getStatus(@PathVariable Integer userId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(gameRewardsService.getStatus(userId)));
        } catch (Exception e) {
            log.error("[API] Error fetching game status for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{userId}/play")
    public ResponseEntity<ApiResponse<GameRewardsService.GamePlayResult>> submitPlay(
            @PathVariable Integer userId,
            @RequestBody PlayRequest request
    ) {
        try {
            GameRewardsService.GamePlayResult result = gameRewardsService.submitPlay(
                    userId,
                    request.getScore(),
                    request.getMoves(),
                    request.getTimeTakenSeconds()
            );
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("[API] Error submitting game play for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<ApiResponse<List<GameRewardsService.GameHistoryItem>>> getHistory(@PathVariable Integer userId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(gameRewardsService.getHistory(userId)));
        } catch (Exception e) {
            log.error("[API] Error fetching game history for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Data
    public static class PlayRequest {
        private Integer score;
        private Integer moves;
        private Integer timeTakenSeconds;
    }
}
