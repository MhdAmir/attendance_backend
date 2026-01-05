package com.backend.repository;

import com.backend.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    @Query("SELECT a FROM Attendance a WHERE a.userId = ?1 AND a.status = 'CHECKED_IN' AND a.checkOutTime IS NULL ORDER BY a.checkInTime DESC")
    Optional<Attendance> findActiveAttendanceByUserId(Long userId);
    
    List<Attendance> findByUserIdAndCheckInTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT a FROM Attendance a WHERE a.syncedToSheets = false ORDER BY a.checkInTime ASC")
    List<Attendance> findUnsyncedAttendances();
    
    @Query("SELECT a FROM Attendance a WHERE FUNCTION('YEAR', a.checkInTime) = ?1 AND FUNCTION('MONTH', a.checkInTime) = ?2 ORDER BY a.fullName, a.checkInTime")
    List<Attendance> findByYearAndMonth(int year, int month);
    
    @Query("SELECT a FROM Attendance a WHERE a.userId = ?1 AND FUNCTION('DATE', a.checkInTime) = FUNCTION('DATE', ?2)")
    Optional<Attendance> findByUserIdAndDate(Long userId, LocalDateTime date);
}
