package com.backend.service;

import com.backend.dto.*;
import com.backend.entity.User;
import com.backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasetoService pasetoService;
    private final BCryptPasswordEncoder passwordEncoder;
    
    public AuthService(UserRepository userRepository, PasetoService pasetoService) {
        this.userRepository = userRepository;
        this.pasetoService = pasetoService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }
    
    /**
     * Register a new user
     */
    @Transactional
    public AuthResponseWithRefresh register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        
        // Save user
        user = userRepository.save(user);
        
        // Generate tokens
        String accessToken = pasetoService.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = pasetoService.generateRefreshToken(user.getId(), user.getUsername());
        
        // Create response
        UserResponse userResponse = UserResponse.fromUser(user);
        AuthResponse authResponse = new AuthResponse(
            accessToken,
            pasetoService.getAccessTokenExpiration(),
            userResponse
        );
        
        return new AuthResponseWithRefresh(authResponse, refreshToken);
    }
    
    /**
     * Login user
     */
    @Transactional(readOnly = true)
    public AuthResponseWithRefresh login(LoginRequest request) {
        // Find user by username or email
        User user = userRepository.findByUsernameOrEmail(
            request.getUsernameOrEmail(),
            request.getUsernameOrEmail()
        ).orElseThrow(() -> new RuntimeException("Invalid username/email or password"));
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username/email or password");
        }
        
        // Generate tokens
        String accessToken = pasetoService.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = pasetoService.generateRefreshToken(user.getId(), user.getUsername());
        
        // Create response
        UserResponse userResponse = UserResponse.fromUser(user);
        AuthResponse authResponse = new AuthResponse(
            accessToken,
            pasetoService.getAccessTokenExpiration(),
            userResponse
        );
        
        return new AuthResponseWithRefresh(authResponse, refreshToken);
    }
    
    /**
     * Refresh access token using refresh token
     */
    @Transactional(readOnly = true)
    public AuthResponseWithRefresh refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        // Validate refresh token
        if (!pasetoService.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        // Extract user information from token
        Long userId = pasetoService.getUserIdFromToken(refreshToken);
        
        // Verify user still exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Generate new tokens
        String newAccessToken = pasetoService.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = pasetoService.generateRefreshToken(user.getId(), user.getUsername());
        
        // Create response
        UserResponse userResponse = UserResponse.fromUser(user);
        AuthResponse authResponse = new AuthResponse(
            newAccessToken,
            pasetoService.getAccessTokenExpiration(),
            userResponse
        );
        
        return new AuthResponseWithRefresh(authResponse, newRefreshToken);
    }
}
