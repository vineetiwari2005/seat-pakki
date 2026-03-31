package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Models.TemporaryWalletCredit;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.TemporaryWalletCreditRepository;
import com.driver.bookMyShow.Repositories.TicketRepository;
import com.driver.bookMyShow.Repositories.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TemporaryWalletCreditService {

    @Value("${app.temp-wallet.validity-days:15}")
    private int tempWalletValidityDays;

    @Autowired
    private TemporaryWalletCreditRepository temporaryWalletCreditRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Transactional
    public TemporaryCreditResult createCredit(Integer userId, Integer ticketId, Double amount) throws Exception {
        return createCredit(userId, ticketId, amount, tempWalletValidityDays, "DATE_CHANGE");
    }

    @Transactional
    public TemporaryCreditResult createCredit(
            Integer userId,
            Integer ticketId,
            Double amount,
            Integer validityDays,
            String sourceType
    ) throws Exception {
        if (amount == null || amount <= 0) {
            throw new Exception("Temporary wallet credit amount must be greater than 0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found with id: " + userId));

        Ticket ticket = null;
        if (ticketId != null) {
            ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new Exception("Ticket not found with ID: " + ticketId));
        }

        LocalDateTime now = LocalDateTime.now();
        int effectiveValidityDays = validityDays == null ? tempWalletValidityDays : validityDays;
        LocalDateTime expiresAt = now.plusDays(Math.max(effectiveValidityDays, 1));

        TemporaryWalletCredit credit = TemporaryWalletCredit.builder()
                .user(user)
                .sourceTicket(ticket)
            .sourceType(sourceType == null || sourceType.isBlank() ? "DATE_CHANGE" : sourceType)
                .totalAmount(round2(amount))
                .remainingAmount(round2(amount))
                .expiresAt(expiresAt)
                .isActive(true)
                .build();

        temporaryWalletCreditRepository.save(credit);

        log.info("Created temporary wallet credit for user {} ticket {} amount {} expiresAt {} sourceType {}",
            userId, ticketId, amount, expiresAt, credit.getSourceType());

        return TemporaryCreditResult.builder()
                .availableAmount(round2(amount))
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public TemporaryCreditResult createGameRewardCredit(Integer userId, Double amount, Integer gameRewardValidityDays) throws Exception {
        return createCredit(userId, null, amount, gameRewardValidityDays, "GAME_REWARD");
    }

    public TemporaryCreditResult getAvailableCredit(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        List<TemporaryWalletCredit> credits = temporaryWalletCreditRepository.findActiveCredits(userId, now);

        double total = credits.stream()
                .mapToDouble(c -> c.getRemainingAmount() == null ? 0.0 : c.getRemainingAmount())
                .sum();

        LocalDateTime nearestExpiry = credits.stream()
                .map(TemporaryWalletCredit::getExpiresAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        return TemporaryCreditResult.builder()
                .availableAmount(round2(total))
                .expiresAt(nearestExpiry)
                .build();
    }

    public List<TemporaryWalletCredit> getCreditHistory(Integer userId) {
        return temporaryWalletCreditRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Double getApplicableCredit(Integer userId, Double billAmount) {
        if (billAmount == null || billAmount <= 0) {
            return 0.0;
        }

        TemporaryCreditResult status = getAvailableCredit(userId);
        return round2(Math.min(status.getAvailableAmount(), billAmount));
    }

    @Transactional
    public Double consumeCredits(Integer userId, Double amountToConsume, String reference) {
        if (amountToConsume == null || amountToConsume <= 0) {
            return 0.0;
        }

        double remaining = round2(amountToConsume);
        LocalDateTime now = LocalDateTime.now();
        List<TemporaryWalletCredit> credits = temporaryWalletCreditRepository.findActiveCreditsForUpdate(userId, now);

        for (TemporaryWalletCredit credit : credits) {
            if (remaining <= 0) {
                break;
            }

            double available = credit.getRemainingAmount() == null ? 0.0 : credit.getRemainingAmount();
            if (available <= 0) {
                continue;
            }

            double toUse = Math.min(available, remaining);
            double newRemaining = round2(available - toUse);

            credit.setRemainingAmount(newRemaining);
            credit.setLastUsedAt(now);
            if (newRemaining <= 0) {
                credit.setIsActive(false);
                credit.setRemainingAmount(0.0);
            }
            temporaryWalletCreditRepository.save(credit);

            remaining = round2(remaining - toUse);
        }

        double consumed = round2(amountToConsume - remaining);
        log.info("Consumed temporary wallet credit for user {} ref {} requested {} consumed {}",
                userId, reference, amountToConsume, consumed);
        return consumed;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Data
    @Builder
    public static class TemporaryCreditResult {
        private Double availableAmount;
        private LocalDateTime expiresAt;
    }
}