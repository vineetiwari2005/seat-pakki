package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Models.RefundRule;
import com.driver.bookMyShow.Repositories.RefundRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * RefundPolicyService - Business logic for refund rules
 * 
 * Design Principles:
 * - Single Responsibility: Only handles refund policy logic
 * - Stateless service (thread-safe)
 * - ALL RULES FROM DATABASE (no hardcoded values)
 * - Open-Closed Principle: Add new rules without code changes
 * 
 * Flow: Service → Repository → Database
 */
@Slf4j
@Service
public class RefundPolicyService {

    @Autowired
    private RefundRuleRepository refundRuleRepository;

    /**
     * Get refund percentage based on hours since booking
     * FETCHES FROM DATABASE ONLY
     * 
     * @param hoursSinceBooking Hours elapsed since booking
     * @return Refund percentage (0-100), 0 if no rule matches
     */
    public int getRefundPercentage(long hoursSinceBooking) {
        log.info("Calculating refund for {} hours since booking", hoursSinceBooking);

        // Fetch all active rules from DB (sorted by hours ascending)
        List<RefundRule> rules = refundRuleRepository.findByIsActiveTrueOrderByHoursThresholdAsc();

        if (rules.isEmpty()) {
            log.warn("No refund rules found in database! Returning 0% refund.");
            return 0;
        }

        // Find first rule where hoursSinceBooking <= hoursThreshold
        for (RefundRule rule : rules) {
            if (hoursSinceBooking <= rule.getHoursThreshold()) {
                log.info("Matched rule: {} hours → {}% refund", rule.getHoursThreshold(), rule.getRefundPercentage());
                return rule.getRefundPercentage();
            }
        }

        // No rule matched - booking too old
        log.info("No matching rule for {} hours. Returning 0% refund.", hoursSinceBooking);
        return 0;
    }

    /**
     * Get all active refund rules (for display/admin)
     * 
     * @return List of active rules
     */
    public List<RefundRule> getAllActiveRules() {
        log.info("Fetching all active refund rules");
        return refundRuleRepository.findByIsActiveTrueOrderByPriorityAsc();
    }

    /**
     * Get policy description (from database rules)
     * 
     * @return Human-readable policy description
     */
    public String getPolicyDescription() {
        List<RefundRule> rules = refundRuleRepository.findByIsActiveTrueOrderByHoursThresholdAsc();

        if (rules.isEmpty()) {
            return "No refund policy configured.";
        }

        StringBuilder sb = new StringBuilder("Refund Policy:\n");
        for (RefundRule rule : rules) {
            sb.append(String.format("- Within %d hour(s) of booking: %d%% refund\n",
                    rule.getHoursThreshold(), rule.getRefundPercentage()));
        }
        sb.append("- After all thresholds: 0% refund");

        return sb.toString();
    }

    /**
     * Create or update refund rule (Admin function)
     * 
     * @param rule RefundRule to save
     * @return Saved rule
     */
    @Transactional
    public RefundRule saveRule(RefundRule rule) {
        log.info("Saving refund rule: {} hours → {}%", rule.getHoursThreshold(), rule.getRefundPercentage());

        // Set defaults
        if (rule.getIsActive() == null) {
            rule.setIsActive(true);
        }
        if (rule.getPriority() == null) {
            rule.setPriority(rule.getHoursThreshold()); // Default: hours = priority
        }

        return refundRuleRepository.save(rule);
    }

    /**
     * Check if cancellation is allowed
     * (Can add DB-driven logic for minimum hours before show)
     * 
     * @param hoursUntilShow Hours until show starts
     * @return true if cancellation allowed
     */
    public boolean isCancellationAllowed(long hoursUntilShow) {
        // For now, allow if show hasn't started
        // Can be enhanced with DB rules later
        return hoursUntilShow >= 0;
    }

    /**
     * Initialize default refund rules (run once on startup if needed)
     * This can be called from @PostConstruct or data.sql
     */
    @Transactional
    public void initializeDefaultRules() {
        if (refundRuleRepository.existsByIsActiveTrue()) {
            log.info("Refund rules already exist. Skipping initialization.");
            return;
        }

        log.info("Initializing default refund rules...");

        // Rule 1: 100% within 1 hour
        saveRule(RefundRule.builder()
                .hoursThreshold(1)
                .refundPercentage(100)
                .description("Full refund within 1 hour of booking")
                .isActive(true)
                .priority(1)
                .build());

        // Rule 2: 75% within 6 hours
        saveRule(RefundRule.builder()
                .hoursThreshold(6)
                .refundPercentage(75)
                .description("75% refund within 6 hours of booking")
                .isActive(true)
                .priority(2)
                .build());

        // Rule 3: 50% within 12 hours
        saveRule(RefundRule.builder()
                .hoursThreshold(12)
                .refundPercentage(50)
                .description("50% refund within 12 hours of booking")
                .isActive(true)
                .priority(3)
                .build());

        log.info("Default refund rules initialized successfully.");
    }
}
