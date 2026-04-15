package com.driver.bookMyShow.Dtos.ResponseDtos;

import com.driver.bookMyShow.Enums.PaymentMethod;
import com.driver.bookMyShow.Enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PaymentResponseDto - Response for payment operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private Integer paymentId;
    private String transactionId;
    private String sessionId;
    private Double baseAmount;
    private Double convenienceFee;
    private Double tax;
    private Double discountAmount;
    private Double totalAmount;
    private Double temporaryWalletAmount;
    private Double payableAmount;
    private Double walletAmount;
    private Double cardAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
