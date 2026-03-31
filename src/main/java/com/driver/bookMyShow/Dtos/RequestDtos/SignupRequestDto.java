package com.driver.bookMyShow.Dtos.RequestDtos;

import com.driver.bookMyShow.Enums.Gender;
import com.driver.bookMyShow.Enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SignupRequestDto - Request DTO for new user registration
 * Extends basic user information with authentication credentials
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String mobileNo;
    
    private Integer age;
    
    private String address;
    
    private Gender gender;

    private Integer cityId;

    // Default role is USER, can be set to THEATER_OWNER during registration
    private UserRole role = UserRole.USER;
}
