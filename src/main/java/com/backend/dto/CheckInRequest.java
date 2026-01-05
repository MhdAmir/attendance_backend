package com.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class CheckInRequest {
    
    @NotBlank(message = "OTP code is required")
    private String otpCode;
    
    // Constructors
    public CheckInRequest() {}
    
    public CheckInRequest(String otpCode) {
        this.otpCode = otpCode;
    }
    
    // Getters and Setters
    public String getOtpCode() {
        return otpCode;
    }
    
    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
