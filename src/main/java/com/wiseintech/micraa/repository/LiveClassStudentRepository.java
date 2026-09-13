package com.wiseintech.micraa.repository;

import com.wiseintech.micraa.model.LiveClassStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveClassStudentRepository extends JpaRepository<LiveClassStudent, LiveClassStudent.LiveClassStudentId> {
    List<LiveClassStudent> findByLiveClassId(Long liveClassId);
    List<LiveClassStudent> findByStudentId(Long studentId);
    boolean existsByLiveClassIdAndStudentId(Long liveClassId, Long studentId);
}
