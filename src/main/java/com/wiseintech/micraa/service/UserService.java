package com.wiseintech.micraa.service;

import com.wiseintech.micraa.dto.UserResponse;
import com.wiseintech.micraa.model.User;
import com.wiseintech.micraa.model.UserRole;
import com.wiseintech.micraa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
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
     * Create a TEACHER account. Only reachable through the ADMIN-protected
     * POST /api/admin/teachers endpoint (see AdminController) - never exposed to public
     * registration or to ordinary authenticated users.
     */
    @Transactional
    public UserResponse createTeacher(String name, String email, String rawPassword) {
        log.info("Admin creating teacher account: {} ({})", name, email);

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

        User user = User.builder()
            .name(name)
            .email(email)
            .passwordHash(passwordEncoder.encode(rawPassword))
            .role(UserRole.TEACHER)
            .build();

        user = userRepository.save(user);
        log.info("Created teacher account with ID: {}", user.getId());

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
