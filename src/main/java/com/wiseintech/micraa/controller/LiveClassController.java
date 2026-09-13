package com.wiseintech.micraa.controller;

import com.wiseintech.micraa.dto.*;
import com.wiseintech.micraa.service.LiveClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/live-classes")
@RequiredArgsConstructor
@Slf4j
public class LiveClassController {
    
    private final LiveClassService liveClassService;
    
    /**
     * Create a new live class
     */
    @PostMapping
    public ResponseEntity<LiveClassResponse> createLiveClass(@RequestBody CreateLiveClassRequest request) {
        log.info("Received request to create live class: {}", request.getTitle());
        LiveClassResponse response = liveClassService.createLiveClass(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get live classes for a user (teacher or student)
     * Query param: userId, role
     */
    @GetMapping
    public ResponseEntity<List<LiveClassResponse>> getLiveClasses(
            @RequestParam Long userId,
            @RequestParam String role) {
        
        log.info("Fetching live classes for user {} with role {}", userId, role);
        
        if ("TEACHER".equals(role)) {
            return ResponseEntity.ok(liveClassService.getTeacherLiveClasses(userId));
        } else if ("STUDENT".equals(role)) {
            return ResponseEntity.ok(liveClassService.getStudentLiveClasses(userId));
        } else {
            throw new RuntimeException("Invalid role");
        }
    }
    
    /**
     * Get a specific live class
     */
    @GetMapping("/{id}")
    public ResponseEntity<LiveClassResponse> getLiveClass(@PathVariable Long id) {
        log.info("Fetching live class {}", id);
        return ResponseEntity.ok(liveClassService.getLiveClass(id));
    }
    
    /**
     * Add students to a live class
     */
    @PostMapping("/{id}/students")
    public ResponseEntity<Void> addStudents(
            @PathVariable Long id,
            @RequestBody AddStudentsRequest request) {
        
        log.info("Adding students to live class {}", id);
        liveClassService.addStudentsToLiveClass(id, request);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Start a live class
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<LiveClassResponse> startLiveClass(
            @PathVariable Long id,
            @RequestParam Long teacherId) {
        
        log.info("Teacher {} starting live class {}", teacherId, id);
        LiveClassResponse response = liveClassService.startLiveClass(id, teacherId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Join a live class (get LiveKit token)
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<JoinLiveClassResponse> joinLiveClass(
            @PathVariable Long id,
            @RequestParam Long userId) {
        
        log.info("User {} joining live class {}", userId, id);
        JoinLiveClassResponse response = liveClassService.joinLiveClass(id, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * End a live class
     */
    @PostMapping("/{id}/end")
    public ResponseEntity<LiveClassResponse> endLiveClass(
            @PathVariable Long id,
            @RequestParam Long teacherId) {
        
        log.info("Teacher {} ending live class {}", teacherId, id);
        LiveClassResponse response = liveClassService.endLiveClass(id, teacherId);
        return ResponseEntity.ok(response);
    }
}
