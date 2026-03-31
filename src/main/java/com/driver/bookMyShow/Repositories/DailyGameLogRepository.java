package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.DailyGameLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyGameLogRepository extends JpaRepository<DailyGameLog, Long> {
    
    @Query("SELECT dgl FROM DailyGameLog dgl WHERE dgl.userId = :userId AND dgl.playedDate = :playedDate")
    Optional<DailyGameLog> findByUserIdAndPlayedDate(@Param("userId") Long userId, @Param("playedDate") LocalDate playedDate);
    
    @Query("SELECT CASE WHEN COUNT(dgl) > 0 THEN true ELSE false END FROM DailyGameLog dgl WHERE dgl.userId = :userId AND dgl.playedDate = :playedDate")
    Boolean hasPlayedToday(@Param("userId") Long userId, @Param("playedDate") LocalDate playedDate);
}
