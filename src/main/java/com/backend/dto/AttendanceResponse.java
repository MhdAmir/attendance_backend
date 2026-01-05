package com.backend.dto;

import com.backend.entity.Attendance;
import java.time.LocalDateTime;
import java.time.Duration;

public class AttendanceResponse {
    
    private Long id;
    private String fullName;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String status;
    private String duration;
    
    // Constructors
    public AttendanceResponse() {}
    
    public AttendanceResponse(Long id, String fullName, LocalDateTime checkInTime, 
                            LocalDateTime checkOutTime, String status, String duration) {
        this.id = id;
        this.fullName = fullName;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.status = status;
        this.duration = duration;
    }
    
    // Factory method to convert Attendance entity to AttendanceResponse
    public static AttendanceResponse fromAttendance(Attendance attendance) {
        String duration = "-";
        if (attendance.getCheckOutTime() != null) {
            long minutes = Duration.between(attendance.getCheckInTime(), attendance.getCheckOutTime()).toMinutes();
            long hours = minutes / 60;
            long mins = minutes % 60;
            duration = String.format("%d:%02d", hours, mins);
        }
        
        return new AttendanceResponse(
            attendance.getId(),
            attendance.getFullName(),
            attendance.getCheckInTime(),
            attendance.getCheckOutTime(),
            attendance.getStatus().name(),
            duration
        );
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }
    
    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }
    
    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }
    
    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDuration() {
        return duration;
    }
    
    public void setDuration(String duration) {
        this.duration = duration;
    }
}
