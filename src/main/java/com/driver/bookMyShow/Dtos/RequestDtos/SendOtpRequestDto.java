package com.driver.bookMyShow.Dtos.RequestDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendOtpRequestDto {

    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    private String referenceId;
}
