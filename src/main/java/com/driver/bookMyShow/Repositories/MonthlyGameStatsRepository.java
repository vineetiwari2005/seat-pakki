package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.MonthlyGameStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyGameStatsRepository extends JpaRepository<MonthlyGameStats, Long> {
    
    Optional<MonthlyGameStats> findByMonthYear(String monthYear);
}
