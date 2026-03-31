package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.TemporaryWalletUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemporaryWalletUsageRepository extends JpaRepository<TemporaryWalletUsage, Long> {
    List<TemporaryWalletUsage> findByUserIdOrderByCreatedAtDesc(Long userId);
}