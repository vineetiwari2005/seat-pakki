package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Models.User;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.List;

/**
 * EmailService - Handles all email notifications
 * 
 * Design Principles:
 * - Asynchronous execution (non-blocking)
 * - Retry-safe (failures don't affect business logic)
 * - Template-based emails
 * - Externalized configuration
 * - No business logic (only notification)
 * 
 * System Design:
 * - Fire-and-forget pattern
 * - Failures logged but not thrown
 * - SMTP configuration externalized to properties
 * - Email templates configurable
 */
@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @Value("${email.booking.enabled:true}")
    private boolean bookingEmailEnabled;

    @Value("${email.wallet.enabled:true}")
    private boolean walletEmailEnabled;

    @Value("${email.cancellation.enabled:true}")
    private boolean cancellationEmailEnabled;

    /**
     * Send booking confirmation email
     * Async - non-blocking operation
     */
    @Async
    public void sendBookingConfirmationEmail(User user, Ticket ticket) {
        if (!bookingEmailEnabled) {
            log.info("Booking email disabled. Skipping email for user: {}", user.getEmailId());
            return;
        }

        try {
            String subject = "Booking Confirmed - BookMyShow";
            String body = buildBookingConfirmationEmail(user, ticket);
            
            sendEmail(user.getEmailId(), subject, body);
            log.info("Booking confirmation email sent to: {}", user.getEmailId());
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email to: {}. Error: {}", 
                     user.getEmailId(), e.getMessage(), e);
            // Don't throw - email failure should not affect booking
        }
    }

    /**
     * Send wallet top-up confirmation email
     * Async - non-blocking operation
     */
    @Async
    public void sendWalletTopUpEmail(User user, Double amount, Double newBalance) {
        if (!walletEmailEnabled) {
            log.info("Wallet email disabled. Skipping email for user: {}", user.getEmailId());
            return;
        }

        try {
            String subject = "Wallet Recharged Successfully - BookMyShow";
            String body = buildWalletTopUpEmail(user, amount, newBalance);
            
            sendEmail(user.getEmailId(), subject, body);
            log.info("Wallet top-up email sent to: {}", user.getEmailId());
        } catch (Exception e) {
            log.error("Failed to send wallet top-up email to: {}. Error: {}", 
                     user.getEmailId(), e.getMessage(), e);
        }
    }

    /**
     * Send booking cancellation email
     * Async - non-blocking operation
     */
    @Async
    public void sendCancellationEmail(User user, Ticket ticket, Double refundAmount) {
        if (!cancellationEmailEnabled) {
            log.info("Cancellation email disabled. Skipping email for user: {}", user.getEmailId());
            return;
        }

        try {
            String subject = "Booking Cancelled - Refund Processed";
            String body = buildCancellationEmail(user, ticket, refundAmount);
            
            sendEmail(user.getEmailId(), subject, body);
            log.info("Cancellation email sent to: {}", user.getEmailId());
        } catch (Exception e) {
            log.error("Failed to send cancellation email to: {}. Error: {}", 
                     user.getEmailId(), e.getMessage(), e);
        }
    }

    /**
     * Send refund notification email
     * Async - non-blocking operation
     */
    @Async
    public void sendRefundNotificationEmail(User user, Double refundAmount, String reason) {
        try {
            String subject = "Refund Processed - BookMyShow";
            String body = buildRefundNotificationEmail(user, refundAmount, reason);
            
            sendEmail(user.getEmailId(), subject, body);
            log.info("Refund notification email sent to: {}", user.getEmailId());
        } catch (Exception e) {
            log.error("Failed to send refund notification email to: {}. Error: {}", 
                     user.getEmailId(), e.getMessage(), e);
        }
    }

    /**
     * Send OTP via email (fallback when SMS provider fails)
     */
    public void sendOtpCodeEmail(String toEmail, String otpCode, String purpose, int validMinutes) throws MessagingException {
        String normalizedPurpose = purpose == null ? "verification" : purpose.trim().toLowerCase();
        String subject = "Your BookMyShow OTP";
        String body = "Your OTP for " + normalizedPurpose + " is " + otpCode + ". It is valid for " + validMinutes + " minutes.";
        sendEmail(toEmail, subject, body);
    }

    /**
     * Core email sending method
     * Private helper - can be upgraded to HTML emails
     */
    private void sendEmail(String to, String subject, String body) throws MessagingException {
        String resolvedUsername = resolveSmtpUsername();
        String resolvedPassword = resolveSmtpPassword();
        validateEmailConfiguration(resolvedUsername, resolvedPassword);
        applyResolvedSmtpCredentials(resolvedUsername, resolvedPassword);

        if (to == null || to.trim().isEmpty()) {
            throw new MessagingException("Recipient email is missing");
        }

        String effectiveFrom = resolvedUsername.trim();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(effectiveFrom);
        message.setTo(to.trim());
        message.setSubject(subject);
        message.setText(body);
        
        mailSender.send(message);
    }

    private void validateEmailConfiguration(String resolvedUsername, String resolvedPassword) throws MessagingException {
        if (resolvedUsername == null || resolvedUsername.trim().isEmpty() ||
                resolvedPassword == null || resolvedPassword.trim().isEmpty()) {
            throw new MessagingException("SMTP credentials are not configured (spring.mail.username/password)");
        }
    }

    private String resolveSmtpUsername() {
        if (smtpUsername != null && !smtpUsername.trim().isEmpty()) {
            return smtpUsername.trim();
        }

        String envMailUser = System.getenv("MAIL_USERNAME");
        if (envMailUser != null && !envMailUser.trim().isEmpty()) {
            return envMailUser.trim();
        }

        String envSmtpUser = System.getenv("SMTP_USERNAME");
        if (envSmtpUser != null && !envSmtpUser.trim().isEmpty()) {
            return envSmtpUser.trim();
        }

        String dotenvMailUser = resolveFromDotenv("MAIL_USERNAME");
        if (dotenvMailUser != null && !dotenvMailUser.trim().isEmpty()) {
            return dotenvMailUser.trim();
        }

        String dotenvSmtpUser = resolveFromDotenv("SMTP_USERNAME");
        if (dotenvSmtpUser != null && !dotenvSmtpUser.trim().isEmpty()) {
            return dotenvSmtpUser.trim();
        }

        String dotenvEmailUser = resolveFromDotenv("EMAIL_USERNAME");
        if (dotenvEmailUser != null && !dotenvEmailUser.trim().isEmpty()) {
            return dotenvEmailUser.trim();
        }

        return "";
    }

    private String resolveSmtpPassword() {
        if (smtpPassword != null && !smtpPassword.trim().isEmpty()) {
            return normalizeSmtpPassword(smtpPassword);
        }

        String envMailPass = System.getenv("MAIL_PASSWORD");
        if (envMailPass != null && !envMailPass.trim().isEmpty()) {
            return normalizeSmtpPassword(envMailPass);
        }

        String envSmtpPass = System.getenv("SMTP_PASSWORD");
        if (envSmtpPass != null && !envSmtpPass.trim().isEmpty()) {
            return normalizeSmtpPassword(envSmtpPass);
        }

        String dotenvMailPass = resolveFromDotenv("MAIL_PASSWORD");
        if (dotenvMailPass != null && !dotenvMailPass.trim().isEmpty()) {
            return normalizeSmtpPassword(dotenvMailPass);
        }

        String dotenvSmtpPass = resolveFromDotenv("SMTP_PASSWORD");
        if (dotenvSmtpPass != null && !dotenvSmtpPass.trim().isEmpty()) {
            return normalizeSmtpPassword(dotenvSmtpPass);
        }

        String dotenvEmailPass = resolveFromDotenv("EMAIL_PASSWORD");
        if (dotenvEmailPass != null && !dotenvEmailPass.trim().isEmpty()) {
            return normalizeSmtpPassword(dotenvEmailPass);
        }

        return "";
    }

    private String normalizeSmtpPassword(String password) {
        if (password == null) {
            return "";
        }
        String trimmed = password.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.replaceAll("\\s+", "");
    }

    private void applyResolvedSmtpCredentials(String username, String password) {
        if (mailSender instanceof JavaMailSenderImpl mailSenderImpl) {
            mailSenderImpl.setUsername(username);
            mailSenderImpl.setPassword(password);
        }
    }

    private String resolveFromDotenv(String key) {
        List<String> searchDirectories = List.of(
                "./",
                "./Book-My-Show",
                "../Book-My-Show"
        );

        for (String directory : searchDirectories) {
            try {
                Dotenv dotenv = Dotenv.configure()
                        .directory(directory)
                        .ignoreIfMissing()
                        .load();
                String value = dotenv.get(key);
                if (value != null && !value.trim().isEmpty()) {
                    return value;
                }
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    /**
     * Send HTML email (for future enhancement)
     */
    private void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        
        mailSender.send(message);
    }

    /**
     * Send ticket email with PDF attachment.
     * Uses the exact PDF bytes generated from frontend download flow.
     */
    public void sendTicketPdfEmail(String to, String subject, String body, byte[] pdfBytes, String fileName)
            throws MessagingException {
        String resolvedUsername = resolveSmtpUsername();
        String resolvedPassword = resolveSmtpPassword();
        validateEmailConfiguration(resolvedUsername, resolvedPassword);
        applyResolvedSmtpCredentials(resolvedUsername, resolvedPassword);

        if (to == null || to.trim().isEmpty()) {
            throw new MessagingException("Recipient email is missing");
        }

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new MessagingException("PDF attachment is empty");
        }

        String effectiveFrom = resolvedUsername.trim();

        String safeFilename = StringUtils.hasText(fileName) ? fileName.trim() : "ticket.pdf";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(effectiveFrom);
        helper.setTo(to.trim());
        helper.setSubject(subject);
        helper.setText(body, false);
        helper.addAttachment(safeFilename, new ByteArrayDataSource(pdfBytes, "application/pdf"));

        mailSender.send(mimeMessage);
    }

    public void sendTicketPdfEmail(String to, String subject, String plainBody, String htmlBody, byte[] pdfBytes, String fileName)
            throws MessagingException {
        String resolvedUsername = resolveSmtpUsername();
        String resolvedPassword = resolveSmtpPassword();
        validateEmailConfiguration(resolvedUsername, resolvedPassword);
        applyResolvedSmtpCredentials(resolvedUsername, resolvedPassword);

        if (to == null || to.trim().isEmpty()) {
            throw new MessagingException("Recipient email is missing");
        }

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new MessagingException("PDF attachment is empty");
        }

        String effectiveFrom = resolvedUsername.trim();

        String safeFilename = StringUtils.hasText(fileName) ? fileName.trim() : "ticket.pdf";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(effectiveFrom);
        helper.setTo(to.trim());
        helper.setSubject(subject);
        helper.setText(plainBody != null ? plainBody : "Please find your ticket PDF attached.",
                htmlBody != null ? htmlBody : "Please find your ticket PDF attached.");
        helper.addAttachment(safeFilename, new ByteArrayDataSource(pdfBytes, "application/pdf"));

        mailSender.send(mimeMessage);
    }

    // ==================== EMAIL TEMPLATES ====================

    private String buildBookingConfirmationEmail(User user, Ticket ticket) {
        return String.format("""
            Dear %s,
            
            Your booking has been confirmed successfully!
            
            Booking Details:
            ================
            Movie: %s
            Theater: %s
            Date: %s
            Time: %s
            Seats: %s
            Total Amount: ₹%d
            
            Booking ID: %d
            Booked At: %s
            
            Show this ticket at the theater entrance.
            
            Thank you for choosing BookMyShow!
            
            Best regards,
            BookMyShow Team
            """,
            user.getName(),
            ticket.getShow().getMovie().getMovieName(),
            ticket.getShow().getTheater().getName(),
            ticket.getShow().getDate(),
            ticket.getShow().getTime(),
            ticket.getBookedSeats(),
            ticket.getTotalTicketsPrice(),
            ticket.getId(),
            ticket.getBookedAt()
        );
    }

    private String buildWalletTopUpEmail(User user, Double amount, Double newBalance) {
        return String.format("""
            Dear %s,
            
            Your wallet has been recharged successfully!
            
            Transaction Details:
            ===================
            Amount Added: ₹%.2f
            New Wallet Balance: ₹%.2f
            
            You can use this balance for booking tickets.
            
            Thank you for using BookMyShow!
            
            Best regards,
            BookMyShow Team
            """,
            user.getName(),
            amount,
            newBalance
        );
    }

    private String buildCancellationEmail(User user, Ticket ticket, Double refundAmount) {
        return String.format("""
            Dear %s,
            
            Your booking has been cancelled successfully.
            
            Cancelled Booking Details:
            ==========================
            Movie: %s
            Theater: %s
            Date: %s
            Time: %s
            Seats: %s
            Original Amount: ₹%d
            
            Refund Details:
            ==============
            Refund Amount: ₹%.2f
            Credited to: Wallet
            New Wallet Balance: ₹%.2f
            
            The refund has been credited to your BookMyShow wallet.
            
            We hope to serve you again soon!
            
            Best regards,
            BookMyShow Team
            """,
            user.getName(),
            ticket.getShow().getMovie().getMovieName(),
            ticket.getShow().getTheater().getName(),
            ticket.getShow().getDate(),
            ticket.getShow().getTime(),
            ticket.getBookedSeats(),
            ticket.getTotalTicketsPrice(),
            refundAmount,
            user.getWalletBalance()
        );
    }

    private String buildRefundNotificationEmail(User user, Double refundAmount, String reason) {
        return String.format("""
            Dear %s,
            
            A refund has been processed to your account.
            
            Refund Details:
            ==============
            Amount: ₹%.2f
            Reason: %s
            Credited to: Wallet
            New Wallet Balance: ₹%.2f
            
            If you have any questions, please contact our support team.
            
            Best regards,
            BookMyShow Team
            """,
            user.getName(),
            refundAmount,
            reason,
            user.getWalletBalance()
        );
    }
}
