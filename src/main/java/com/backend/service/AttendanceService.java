package com.backend.service;

import com.backend.dto.AttendanceResponse;
import com.backend.entity.Attendance;
import com.backend.entity.User;
import com.backend.repository.AttendanceRepository;
import com.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceService {
    
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final OTPService otpService;
    private final GoogleSheetsService googleSheetsService;
    
    public AttendanceService(AttendanceRepository attendanceRepository,
                           UserRepository userRepository,
                           OTPService otpService,
                           GoogleSheetsService googleSheetsService) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.googleSheetsService = googleSheetsService;
    }
    
    /**
     * Check in with OTP
     */
    @Transactional
    public AttendanceResponse checkIn(Long userId, String otpCode) {
        // Verify OTP
        if (!otpService.verifyOTP(otpCode)) {
            throw new RuntimeException("Invalid OTP code");
        }
        
        // Check if user already has attendance for today
        LocalDateTime today = LocalDateTime.now();
        attendanceRepository.findByUserIdAndDate(userId, today)
                .ifPresent(a -> {
                    throw new RuntimeException("You have already checked in today. Only one check-in per day is allowed.");
                });
        
        // Check if user already checked in
        attendanceRepository.findActiveAttendanceByUserId(userId)
                .ifPresent(a -> {
                    throw new RuntimeException("You are already checked in. Please check out first.");
                });
        
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Create attendance record
        Attendance attendance = new Attendance(userId, user.getFullName(), otpCode);
        attendance = attendanceRepository.save(attendance);
        
        // Sync to Google Sheets asynchronously
        syncToGoogleSheetsAsync(attendance);
        
        return AttendanceResponse.fromAttendance(attendance);
    }
    
    /**
     * Check out with OTP
     */
    @Transactional
    public AttendanceResponse checkOut(Long userId, String otpCode) {
        // Verify OTP
        if (!otpService.verifyOTP(otpCode)) {
            throw new RuntimeException("Invalid OTP code");
        }
        
        // Find active attendance
        Attendance attendance = attendanceRepository.findActiveAttendanceByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No active check-in found. Please check in first."));
        
        // Update check-out time
        attendance.setCheckOutTime(LocalDateTime.now());
        attendance.setStatus(Attendance.AttendanceStatus.CHECKED_OUT);
        attendance = attendanceRepository.save(attendance);
        
        // Update Google Sheets asynchronously
        updateGoogleSheetsAsync(attendance);
        
        return AttendanceResponse.fromAttendance(attendance);
    }
    
    /**
     * Get user's attendance history
     */
    public List<AttendanceResponse> getUserAttendanceHistory(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Attendance> attendances = attendanceRepository.findByUserIdAndCheckInTimeBetween(userId, start, end);
        return attendances.stream()
                .map(AttendanceResponse::fromAttendance)
                .collect(Collectors.toList());
    }
    
    /**
     * Get current user's active attendance
     */
    public AttendanceResponse getCurrentAttendance(Long userId) {
        return attendanceRepository.findActiveAttendanceByUserId(userId)
                .map(AttendanceResponse::fromAttendance)
                .orElse(null);
    }
    
    /**
     * Sync to Google Sheets (async)
     */
    private void syncToGoogleSheetsAsync(Attendance attendance) {
        new Thread(() -> {
            try {
                googleSheetsService.writeAttendance(
                    attendance.getFullName(),
                    attendance.getCheckInTime(),
                    attendance.getCheckOutTime()
                );
                attendance.setSyncedToSheets(true);
                attendanceRepository.save(attendance);
            } catch (Exception e) {
                System.err.println("Failed to sync to Google Sheets: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Update Google Sheets (async)
     */
    private void updateGoogleSheetsAsync(Attendance attendance) {
        new Thread(() -> {
            try {
                googleSheetsService.updateAttendance(
                    attendance.getFullName(),
                    attendance.getCheckInTime(),
                    attendance.getCheckOutTime()
                );
                attendance.setSyncedToSheets(true);
                attendanceRepository.save(attendance);
            } catch (Exception e) {
                System.err.println("Failed to update Google Sheets: " + e.getMessage());
            }
        }).start();
    }
}
