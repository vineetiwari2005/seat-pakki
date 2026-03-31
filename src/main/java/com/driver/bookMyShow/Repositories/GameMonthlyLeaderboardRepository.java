package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.GameMonthlyLeaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameMonthlyLeaderboardRepository extends JpaRepository<GameMonthlyLeaderboard, String> {
}
