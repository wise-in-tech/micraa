package com.wiseintech.micraa.controller;

import com.wiseintech.micraa.dto.UserResponse;
import com.wiseintech.micraa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    @GetMapping("/teachers")
    public ResponseEntity<List<UserResponse>> getAllTeachers() {
        return ResponseEntity.ok(userService.getAllTeachers());
    }
    
    @GetMapping("/students")
    public ResponseEntity<List<UserResponse>> getAllStudents() {
        return ResponseEntity.ok(userService.getAllStudents());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    // The legacy "POST /api/users" endpoint that let any authenticated caller create a
    // user with an arbitrary role (including TEACHER) has been removed. Teacher accounts
    // are now created exclusively through the ADMIN-only "POST /api/admin/teachers"
    // endpoint (see AdminController).
}
