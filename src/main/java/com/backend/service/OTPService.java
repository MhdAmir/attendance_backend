package com.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Service
public class OTPService {
    
    @Value("${otp.secret.hex}")
    private String otpSecretHex;
    
    @Value("${otp.time.step.seconds:300}") // 300 seconds (5 minutes) - matching C++
    private long timeStepSeconds;
    
    /**
     * Convert hex string to byte array
     */
    private byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }
    
    /**
     * Verify OTP code from robot
     * OTP format: 6-digit code based on TOTP algorithm with HMAC-SHA256
     * Strict mode: Only accepts current time window (no tolerance for clock drift)
     */
    public boolean verifyOTP(String otpCode) {
        if (otpCode == null || otpCode.length() != 6) {
            return false;
        }
        
        try {
            long currentTime = Instant.now().getEpochSecond() / timeStepSeconds;
            
            // Strict mode: Only check current time window
            String generatedOTP = generateTOTP(currentTime);
            return otpCode.equals(generatedOTP);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Generate current OTP (for testing/debugging)
     */
    public String getCurrentOTP() {
        long currentTime = Instant.now().getEpochSecond() / timeStepSeconds;
        return generateTOTP(currentTime);
    }
    
    /**
     * Get seconds remaining in current time window
     */
    public long getSecondsRemaining() {
        long currentTime = Instant.now().getEpochSecond();
        long remainder = currentTime % timeStepSeconds;
        return timeStepSeconds - remainder;
    }
    
    /**
     * Generate TOTP code using HMAC-SHA256 (matching C++ implementation)
     */
    private String generateTOTP(long timeCounter) {
        try {
            // Convert hex secret to bytes
            byte[] key = hexToBytes(otpSecretHex);
            byte[] data = ByteBuffer.allocate(8).putLong(timeCounter).array();
            
            // Use HMAC-SHA256 (matching C++ implementation)
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] hash = mac.doFinal(data);
            
            // Dynamic truncation (RFC 4226)
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24) |
                        ((hash[offset + 1] & 0xFF) << 16) |
                        ((hash[offset + 2] & 0xFF) << 8) |
                        (hash[offset + 3] & 0xFF);
            
            int otp = binary % 1000000;
            return String.format("%06d", otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate OTP", e);
        }
    }
}
