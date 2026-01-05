package com.backend.controller;

import com.backend.dto.*;
import com.backend.service.AttendanceService;
import com.backend.service.OTPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@Tag(name = "Attendance", description = "Attendance management APIs with OTP verification")
@SecurityRequirement(name = "Bearer Authentication")
public class AttendanceController {
    
    private final AttendanceService attendanceService;
    private final OTPService otpService;
    
    public AttendanceController(AttendanceService attendanceService, OTPService otpService) {
        this.attendanceService = attendanceService;
        this.otpService = otpService;
    }
    
    @PostMapping("/check-in")
    @Operation(
        summary = "Check in with OTP",
        description = "Record attendance check-in using OTP code from offline robot"
    )
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(
            @Valid @RequestBody CheckInRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            AttendanceResponse response = attendanceService.checkIn(userId, request.getOtpCode());
            return ResponseEntity.ok(ApiResponse.success("Check-in successful", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/check-out")
    @Operation(
        summary = "Check out with OTP",
        description = "Record attendance check-out using OTP code from offline robot"
    )
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(
            @Valid @RequestBody CheckOutRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            AttendanceResponse response = attendanceService.checkOut(userId, request.getOtpCode());
            return ResponseEntity.ok(ApiResponse.success("Check-out successful", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/current")
    @Operation(
        summary = "Get current attendance",
        description = "Get current user's active attendance session"
    )
    public ResponseEntity<ApiResponse<AttendanceResponse>> getCurrentAttendance(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        AttendanceResponse response = attendanceService.getCurrentAttendance(userId);
        
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.success("No active attendance", null));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Current attendance retrieved", response));
    }
    
    @GetMapping("/history")
    @Operation(
        summary = "Get attendance history",
        description = "Get user's attendance history for a date range"
    )
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getHistory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        
        LocalDateTime start = startDate != null ? 
                LocalDateTime.parse(startDate) : 
                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        
        LocalDateTime end = endDate != null ? 
                LocalDateTime.parse(endDate) : 
                LocalDateTime.now();
        
        List<AttendanceResponse> history = attendanceService.getUserAttendanceHistory(userId, start, end);
        return ResponseEntity.ok(ApiResponse.success("Attendance history retrieved", history));
    }
    
    @GetMapping("/otp/current")
    @Operation(
        summary = "Get current OTP (for testing)",
        description = "Get current valid OTP code - for development/testing only"
    )
    public ResponseEntity<ApiResponse<OTPInfo>> getCurrentOTP() {
        String otp = otpService.getCurrentOTP();
        long remaining = otpService.getSecondsRemaining();
        
        OTPInfo info = new OTPInfo(otp, remaining);
        return ResponseEntity.ok(ApiResponse.success("Current OTP code", info));
    }
    
    // Inner class for OTP info
    public static class OTPInfo {
        private String otpCode;
        private long remainingSeconds;
        
        public OTPInfo(String otpCode, long remainingSeconds) {
            this.otpCode = otpCode;
            this.remainingSeconds = remainingSeconds;
        }
        
        public String getOtpCode() { return otpCode; }
        public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
        public long getRemainingSeconds() { return remainingSeconds; }
        public void setRemainingSeconds(long remainingSeconds) { this.remainingSeconds = remainingSeconds; }
    }
}
