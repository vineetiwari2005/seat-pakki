package com.driver.bookMyShow.Dtos.RequestDtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateProfileDto - DTO for updating user profile credentials
 * 
 * Allows user to update:
 * - Name
 * - Email
 * - Mobile number
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileDto {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNo;

    private String otpCode;

    private String otpRequestId;
}
