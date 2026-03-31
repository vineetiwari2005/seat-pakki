package com.driver.bookMyShow.Dtos.RequestDtos;

import com.driver.bookMyShow.Enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PaymentInitiationDto - Request to initiate payment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiationDto {

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double baseAmount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String promoCode;

    private Boolean useTemporaryWallet = true;
}
