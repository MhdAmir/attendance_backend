package com.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class CheckOutRequest {
    
    @NotBlank(message = "OTP code is required")
    private String otpCode;
    
    // Constructors
    public CheckOutRequest() {}
    
    public CheckOutRequest(String otpCode) {
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
