package com.driver.bookMyShow.Controllers;

import com.driver.bookMyShow.Dtos.RequestDtos.LoginRequestDto;
import com.driver.bookMyShow.Dtos.RequestDtos.SignupRequestDto;
import com.driver.bookMyShow.Dtos.ResponseDtos.AuthResponseDto;
import com.driver.bookMyShow.Exceptions.UserAlreadyExistsWithEmail;
import com.driver.bookMyShow.Services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthController - Handles authentication endpoints
 * Provides signup, login, and token refresh functionality
 * 
 * NEW ENDPOINTS - Does not affect existing functionality
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * User Signup
     * POST /auth/signup
     * 
     * Creates a new user account with encrypted password
     * Returns JWT tokens for immediate authentication
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequestDto signupRequest) {
        try {
            AuthResponseDto response = authService.signup(signupRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserAlreadyExistsWithEmail e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User already exists with this email");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

    /**
     * User Login
     * POST /auth/login
     * 
     * Authenticates user and returns JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        try {
            AuthResponseDto response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Refresh Access Token
     * POST /auth/refresh
     * 
     * Generates new access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Refresh token is required");
                return ResponseEntity.badRequest().body(error);
            }

            AuthResponseDto response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Health check endpoint
     * GET /auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Authentication service is running");
        return ResponseEntity.ok(response);
    }
    /*
     
     */
}
