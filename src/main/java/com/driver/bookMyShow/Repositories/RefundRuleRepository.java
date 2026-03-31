package com.driver.bookMyShow.Repositories;

import com.driver.bookMyShow.Models.RefundRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RefundRuleRepository - Data access for refund rules
 * 
 * Design Principles:
 * - Spring Data JPA naming conventions
 * - No hardcoded queries
 * - Repository pattern (no business logic)
 */
@Repository
public interface RefundRuleRepository extends JpaRepository<RefundRule, Integer> {

    /**
     * Find all active rules ordered by priority (ascending)
     * Lower priority number = higher priority
     * 
     * @return List of active refund rules
     */
    List<RefundRule> findByIsActiveTrueOrderByPriorityAsc();

    /**
     * Find all active rules ordered by hours threshold (ascending)
     * Used for sequential matching logic
     * 
     * @return List of active rules sorted by hours
     */
    List<RefundRule> findByIsActiveTrueOrderByHoursThresholdAsc();

    /**
     * Find rule by exact hours threshold
     * 
     * @param hoursThreshold Hours after booking
     * @return RefundRule if found
     */
    Optional<RefundRule> findByHoursThresholdAndIsActiveTrue(Integer hoursThreshold);

    /**
     * Find applicable rule for given hours since booking
     * Returns the rule with highest threshold that doesn't exceed the given hours
     * 
     * @param hoursSinceBooking Hours elapsed since booking
     * @return First matching active rule
     */
    @Query("SELECT r FROM RefundRule r " +
           "WHERE r.isActive = true " +
           "AND r.hoursThreshold >= :hoursSinceBooking " +
           "ORDER BY r.hoursThreshold ASC")
    Optional<RefundRule> findApplicableRule(@Param("hoursSinceBooking") long hoursSinceBooking);

    /**
     * Check if any active rules exist
     * 
     * @return true if at least one active rule exists
     */
    boolean existsByIsActiveTrue();
}
