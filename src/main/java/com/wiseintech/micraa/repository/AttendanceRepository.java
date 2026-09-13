package com.wiseintech.micraa.repository;

import com.wiseintech.micraa.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByLiveClassId(Long liveClassId);
    List<Attendance> findByStudentId(Long studentId);
    Optional<Attendance> findByLiveClassIdAndStudentIdAndLeftAtIsNull(Long liveClassId, Long studentId);
}
