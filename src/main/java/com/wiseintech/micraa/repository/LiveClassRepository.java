package com.wiseintech.micraa.repository;

import com.wiseintech.micraa.model.LiveClass;
import com.wiseintech.micraa.model.LiveClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveClassRepository extends JpaRepository<LiveClass, Long> {
    List<LiveClass> findByTeacherId(Long teacherId);
    List<LiveClass> findByStatus(LiveClassStatus status);
    List<LiveClass> findByTeacherIdAndStatus(Long teacherId, LiveClassStatus status);
}
