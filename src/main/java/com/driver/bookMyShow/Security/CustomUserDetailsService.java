package com.driver.bookMyShow.Security;

import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * CustomUserDetailsService - Loads user-specific data for Spring Security
 * Integrates existing User entity with Spring Security
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailId(email);
        
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("User account is inactive");
        }

        // Handle legacy users without password (for backward compatibility)
        String password = user.getPassword() != null ? user.getPassword() : "";

        // Convert our User to Spring Security UserDetails
        return new org.springframework.security.core.userdetails.User(
                user.getEmailId(),
                password,
                user.getIsActive(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                )
        );
    }

    /**
     * Get user entity by email
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmailId(email);
    }
}
