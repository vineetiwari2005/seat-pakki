package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Dtos.RequestDtos.LoginRequestDto;
import com.driver.bookMyShow.Dtos.RequestDtos.SignupRequestDto;
import com.driver.bookMyShow.Dtos.ResponseDtos.AuthResponseDto;
import com.driver.bookMyShow.Exceptions.UserAlreadyExistsWithEmail;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Models.UserWallet;
import com.driver.bookMyShow.Repositories.UserRepository;
import com.driver.bookMyShow.Repositories.UserWalletRepository;
import com.driver.bookMyShow.Utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * AuthService - Handles user authentication and registration
 * Provides JWT-based login and signup functionality
 */
@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserWalletRepository userWalletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private MovieShowRedisCacheService movieShowRedisCacheService;

    /**
     * Register a new user
     * Creates user account with hashed password
     */
    public AuthResponseDto signup(SignupRequestDto signupRequest) throws UserAlreadyExistsWithEmail {
        // Check if user already exists
        if (userRepository.findByEmailId(signupRequest.getEmail()) != null) {
            throw new UserAlreadyExistsWithEmail();
        }

        // Create new user
        User user = User.builder()
                .name(signupRequest.getName())
                .emailId(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .mobileNo(signupRequest.getMobileNo())
                .age(signupRequest.getAge())
                .address(signupRequest.getAddress())
                .gender(signupRequest.getGender())
                .role(signupRequest.getRole())
                .cityId(signupRequest.getCityId())
                .isActive(true)
                .walletBalance(10000.0) // Set initial balance to 10,000
                .build();

        user = userRepository.save(user);
        
        // Create wallet with 10,000 default balance for new user (DB-driven)
        UserWallet wallet = UserWallet.builder()
            .user(user)
            .balance(10000.0)
            .build();
        userWalletRepository.save(wallet);

        // Generate tokens
        String accessToken = jwtUtil.generateToken(user.getEmailId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmailId());

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(10 * 60 * 60L) // 10 hours in seconds
                .email(user.getEmailId())
                .name(user.getName())
                .role(user.getRole().name())
                .userId(user.getId())
                .cityId(user.getCityId())
                .build();
    }

    /**
     * Login existing user
     * Validates credentials and returns JWT tokens
     */
    public AuthResponseDto login(LoginRequestDto loginRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Get user details
            User user = userRepository.findByEmailId(loginRequest.getEmail());
            if (user == null) {
                throw new UsernameNotFoundException("User not found");
            }

            // Generate tokens
            String accessToken = jwtUtil.generateToken(user.getEmailId(), user.getRole().name());
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmailId());

            // Pre-warm Redis cache for today + next 3 days on login
            try {
                movieShowRedisCacheService.ensureFourDayCache();
            } catch (Exception e) {
                log.warn("Redis cache warm-up failed on login: {}", e.getMessage());
            }

            return AuthResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(10 * 60 * 60L) // 10 hours in seconds
                    .email(user.getEmailId())
                    .name(user.getName())
                    .role(user.getRole().name())
                    .userId(user.getId())
                    .cityId(user.getCityId())
                    .build();

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponseDto refreshToken(String refreshToken) {
        try {
            String email = jwtUtil.extractUsername(refreshToken);
            User user = userRepository.findByEmailId(email);

            if (user == null) {
                throw new UsernameNotFoundException("User not found");
            }

            if (jwtUtil.validateToken(refreshToken, email)) {
                String newAccessToken = jwtUtil.generateToken(email, user.getRole().name());

                return AuthResponseDto.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(refreshToken)
                        .tokenType("Bearer")
                        .expiresIn(10 * 60 * 60L)
                        .email(user.getEmailId())
                        .name(user.getName())
                        .role(user.getRole().name())
                        .userId(user.getId())
                        .cityId(user.getCityId())
                        .build();
            } else {
                throw new BadCredentialsException("Invalid refresh token");
            }
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid refresh token");
        }
    }
}
