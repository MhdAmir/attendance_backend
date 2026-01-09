package com.backend.controller;

import com.backend.dto.*;
import com.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Create a new user account with username, email, password, and full name"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input or user already exists"
        )
    })
    public ResponseEntity<com.backend.dto.ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            BindingResult bindingResult,
            HttpServletResponse response) {
        
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest()
                    .body(com.backend.dto.ApiResponse.error(errors));
        }
        
        try {
            AuthResponseWithRefresh result = authService.register(request);
            
            // Set refresh token in httpOnly cookie
            setRefreshTokenCookie(response, result.getRefreshToken());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(com.backend.dto.ApiResponse.success("User registered successfully", result.getAuthResponse()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(com.backend.dto.ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    @Operation(
        summary = "Login user",
        description = "Authenticate user with username/email and password, returns access and refresh tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials"
        )
    })
    public ResponseEntity<com.backend.dto.ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            BindingResult bindingResult,
            HttpServletResponse response) {
        
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest()
                    .body(com.backend.dto.ApiResponse.error(errors));
        }
        
        try {
            AuthResponseWithRefresh result = authService.login(request);
            
            // Set refresh token in httpOnly cookie
            setRefreshTokenCookie(response, result.getRefreshToken());
            
            return ResponseEntity.ok(
                    com.backend.dto.ApiResponse.success("Login successful", result.getAuthResponse()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(com.backend.dto.ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Generate new access and refresh tokens using refresh token from httpOnly cookie"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid refresh token"
        )
    })
    public ResponseEntity<com.backend.dto.ApiResponse<AuthResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        try {
            // Get refresh token from cookie
            String refreshToken = getRefreshTokenFromCookie(request);
            
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.backend.dto.ApiResponse.error("Refresh token not found"));
            }
            
            RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
            AuthResponseWithRefresh result = authService.refreshToken(refreshRequest);
            
            // Set new refresh token in httpOnly cookie
            setRefreshTokenCookie(response, result.getRefreshToken());
            
            return ResponseEntity.ok(
                    com.backend.dto.ApiResponse.success("Token refreshed successfully", result.getAuthResponse()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(com.backend.dto.ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Clear refresh token cookie"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Logout successful"
        )
    })
    public ResponseEntity<com.backend.dto.ApiResponse<Void>> logout(HttpServletResponse response) {
        // Clear refresh token cookie
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Delete cookie
        cookie.setAttribute("SameSite", "None");
        response.addCookie(cookie);
        
        return ResponseEntity.ok(
                com.backend.dto.ApiResponse.success("Logout successful", null));
    }
    
    /**
     * Set refresh token in httpOnly cookie
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Set HTTPS in production
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        cookie.setAttribute("SameSite", "None"); // requires HTTPS
        response.addCookie(cookie);
    }
    
    /**
     * Get refresh token from httpOnly cookie
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
