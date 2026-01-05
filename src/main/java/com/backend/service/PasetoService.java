package com.backend.service;

import dev.paseto.jpaseto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PasetoService {
    
    @Value("${paseto.secret.key}")
    private String secretKeyString;
    
    @Value("${paseto.access.token.expiration:900}") // 15 minutes in seconds
    private long accessTokenExpiration;
    
    @Value("${paseto.refresh.token.expiration:604800}") // 7 days in seconds
    private long refreshTokenExpiration;
    
    private static final String ISSUER = "eros-attendance-api";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    
    /**
     * Generate access token for authenticated user
     */
    public String generateAccessToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenExpiration, ChronoUnit.SECONDS);
        
        SecretKey key = getSecretKey();
        
        return Pasetos.V2.LOCAL.builder()
                .setSharedSecret(key)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .setIssuer(ISSUER)
                .setSubject(userId.toString())
                .claim("username", username)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .compact();
    }
    
    /**
     * Generate refresh token for authenticated user
     */
    public String generateRefreshToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenExpiration, ChronoUnit.SECONDS);
        
        SecretKey key = getSecretKey();
        
        return Pasetos.V2.LOCAL.builder()
                .setSharedSecret(key)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .setIssuer(ISSUER)
                .setSubject(userId.toString())
                .claim("username", username)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .compact();
    }
    
    /**
     * Validate and parse PASETO token
     */
    public Paseto validateAndParseToken(String token) {
        try {
            SecretKey key = getSecretKey();
            
            return Pasetos.parserBuilder()
                    .setSharedSecret(key)
                    .requireIssuer(ISSUER)
                    .build()
                    .parse(token);
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired token: " + e.getMessage());
        }
    }
    
    /**
     * Extract user ID from token
     */
    public Long getUserIdFromToken(String token) {
        Paseto parsedToken = validateAndParseToken(token);
        String subject = parsedToken.getClaims().getSubject();
        return Long.parseLong(subject);
    }
    
    /**
     * Extract username from token
     */
    public String getUsernameFromToken(String token) {
        Paseto parsedToken = validateAndParseToken(token);
        return parsedToken.getClaims().get("username", String.class);
    }
    
    /**
     * Check if token is access token
     */
    public boolean isAccessToken(String token) {
        try {
            Paseto parsedToken = validateAndParseToken(token);
            String tokenType = parsedToken.getClaims().get(TOKEN_TYPE_CLAIM, String.class);
            return ACCESS_TOKEN_TYPE.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if token is refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Paseto parsedToken = validateAndParseToken(token);
            String tokenType = parsedToken.getClaims().get(TOKEN_TYPE_CLAIM, String.class);
            return REFRESH_TOKEN_TYPE.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get access token expiration in seconds
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
    
    /**
     * Get refresh token expiration in seconds
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
    
    /**
     * Generate secret key from configured string
     * PASETO v2.local requires exactly 32 bytes
     */
    private SecretKey getSecretKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secretKeyString.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate secret key: " + e.getMessage());
        }
    }
}
