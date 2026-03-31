package com.driver.bookMyShow.Gateway;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * MockPaymentGateway - Simulates external payment gateway
 * 
 * In production, replace with actual payment gateway integration:
 * - Razorpay
 * - Stripe
 * - PayPal
 * - Paytm
 * 
 * Current behavior:
 * - 90% success rate for realistic simulation
 * - Generates mock transaction IDs
 * - Simulates payment processing delay
 */
@Component
public class MockPaymentGateway {

    private final Random random = new Random();

    /**
     * Process payment through mock gateway
     * 
     * @param amount Amount to charge
     * @param paymentMethod Payment method
     * @param userEmail User email
     * @return Payment response with status and transaction ID
     */
    public PaymentGatewayResponse processPayment(Double amount, String paymentMethod, String userEmail) {
        // Simulate processing delay
        try {
            Thread.sleep(1000 + random.nextInt(2000)); // 1-3 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Generate mock transaction ID
        String gatewayTransactionId = "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 90% success rate
        boolean isSuccess = random.nextInt(100) < 90;

        PaymentGatewayResponse response = new PaymentGatewayResponse();
        response.setTransactionId(gatewayTransactionId);
        response.setSuccess(isSuccess);
        response.setAmount(amount);

        if (isSuccess) {
            response.setMessage("Payment processed successfully");
            response.setStatusCode("SUCCESS");
        } else {
            // Random failure reasons
            String[] failureReasons = {
                "Insufficient funds",
                "Card declined",
                "Transaction timeout",
                "Invalid card details"
            };
            response.setMessage(failureReasons[random.nextInt(failureReasons.length)]);
            response.setStatusCode("FAILED");
        }

        return response;
    }

    /**
     * Verify payment status (for idempotency check)
     */
    public PaymentGatewayResponse verifyPayment(String gatewayTransactionId) {
        PaymentGatewayResponse response = new PaymentGatewayResponse();
        response.setTransactionId(gatewayTransactionId);
        response.setSuccess(true);
        response.setMessage("Payment verification successful");
        response.setStatusCode("SUCCESS");
        return response;
    }

    /**
     * Process refund through mock gateway
     */
    public RefundResponse processRefund(String gatewayTransactionId, Double amount) {
        // Simulate processing delay
        try {
            Thread.sleep(500 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String refundId = "REF_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        RefundResponse response = new RefundResponse();
        response.setRefundId(refundId);
        response.setOriginalTransactionId(gatewayTransactionId);
        response.setAmount(amount);
        response.setSuccess(true);
        response.setMessage("Refund processed successfully");

        return response;
    }

    /**
     * Payment Gateway Response DTO
     */
    public static class PaymentGatewayResponse {
        private String transactionId;
        private boolean success;
        private String message;
        private String statusCode;
        private Double amount;

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getStatusCode() { return statusCode; }
        public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
    }

    /**
     * Refund Response DTO
     */
    public static class RefundResponse {
        private String refundId;
        private String originalTransactionId;
        private Double amount;
        private boolean success;
        private String message;

        public String getRefundId() { return refundId; }
        public void setRefundId(String refundId) { this.refundId = refundId; }
        
        public String getOriginalTransactionId() { return originalTransactionId; }
        public void setOriginalTransactionId(String originalTransactionId) { 
            this.originalTransactionId = originalTransactionId; 
        }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
