package com.backend.dto;

/**
 * Internal DTO to pass both AuthResponse and refreshToken from service to controller
 * The refreshToken will be set as httpOnly cookie, not sent in response body
 */
public class AuthResponseWithRefresh {
    
    private AuthResponse authResponse;
    private String refreshToken;
    
    public AuthResponseWithRefresh(AuthResponse authResponse, String refreshToken) {
        this.authResponse = authResponse;
        this.refreshToken = refreshToken;
    }
    
    public AuthResponse getAuthResponse() {
        return authResponse;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
}
