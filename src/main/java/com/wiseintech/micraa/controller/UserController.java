package com.wiseintech.micraa.controller;

import com.wiseintech.micraa.dto.UserResponse;
import com.wiseintech.micraa.model.UserRole;
import com.wiseintech.micraa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    
    /**
     * Temporary endpoint to create users for testing
     * Will be replaced with proper authentication
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");
        UserRole role = UserRole.valueOf(request.get("role"));
        
        UserResponse user = userService.createUser(name, email, role);
        return ResponseEntity.ok(user);
    }
}
