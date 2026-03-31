package com.driver.bookMyShow.Dtos.ResponseDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AuthResponseDto - Response DTO for authentication operations
 * Contains JWT tokens and user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private String email;
    private String name;
    private String role;
    private Integer userId;
    private Integer cityId;
}
