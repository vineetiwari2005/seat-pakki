package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.GamePlayLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GamePlayLogRepository extends JpaRepository<GamePlayLog, Integer> {
    boolean existsByUserIdAndPlayedDate(Integer userId, LocalDate playedDate);
    Optional<GamePlayLog> findByUserIdAndPlayedDate(Integer userId, LocalDate playedDate);
    List<GamePlayLog> findByUserIdOrderByPlayedAtDesc(Integer userId);
}
