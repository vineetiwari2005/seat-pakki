package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.GameLeaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameLeaderboardRepository extends JpaRepository<GameLeaderboard, Long> {
    
    @Query("SELECT gl FROM GameLeaderboard gl WHERE gl.userId = :userId AND gl.monthYear = :monthYear")
    Optional<GameLeaderboard> findByUserIdAndMonthYear(@Param("userId") Long userId, @Param("monthYear") String monthYear);
    
    @Query("SELECT gl FROM GameLeaderboard gl WHERE gl.monthYear = :monthYear ORDER BY gl.highestScore DESC")
    List<GameLeaderboard> findTopScoresByMonth(@Param("monthYear") String monthYear);
    
    @Query("SELECT MAX(gl.highestScore) FROM GameLeaderboard gl WHERE gl.monthYear = :monthYear")
    Integer getHighestScoreOfMonth(@Param("monthYear") String monthYear);
    
    @Query("SELECT AVG(gl.highestScore) FROM GameLeaderboard gl WHERE gl.monthYear = :monthYear")
    Double getAverageScoreOfMonth(@Param("monthYear") String monthYear);
}
