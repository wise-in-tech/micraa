package com.wiseintech.micraa.service;

import com.wiseintech.micraa.dto.UserResponse;
import com.wiseintech.micraa.model.User;
import com.wiseintech.micraa.model.UserRole;
import com.wiseintech.micraa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * Get all users
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::toUserResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all teachers
     */
    public List<UserResponse> getAllTeachers() {
        return userRepository.findByRole(UserRole.TEACHER).stream()
            .map(this::toUserResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all students
     */
    public List<UserResponse> getAllStudents() {
        return userRepository.findByRole(UserRole.STUDENT).stream()
            .map(this::toUserResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get a user by ID
     */
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return toUserResponse(user);
    }
    
    /**
     * Create a simple user (for testing - no real authentication yet)
     */
    @Transactional
    public UserResponse createUser(String name, String email, UserRole role) {
        log.info("Creating user: {} with email: {} and role: {}", name, email, role);
        
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }
        
        User user = User.builder()
            .name(name)
            .email(email)
            .passwordHash("temp-hash") // Temporary - will implement proper auth later
            .role(role)
            .build();
        
        user = userRepository.save(user);
        log.info("Created user with ID: {}", user.getId());
        
        return toUserResponse(user);
    }
    
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole())
            .build();
    }
}
