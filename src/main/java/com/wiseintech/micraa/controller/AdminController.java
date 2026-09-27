package com.wiseintech.micraa.controller;

import com.wiseintech.micraa.dto.CreateTeacherRequest;
import com.wiseintech.micraa.dto.UserResponse;
import com.wiseintech.micraa.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Administrative endpoints. Every method here requires the caller's JWT to carry the
 * ADMIN role - never a role supplied in the request body, only the role stored on the
 * authenticated user's account.
 *
 * V1 has no API endpoint that can create an ADMIN: the first ADMIN account is seeded
 * directly in the database (operator-run SQL, out of scope of this codebase) with a
 * bcrypt password hash in the `users` table and role = 'ADMIN'. From that point on, that
 * ADMIN logs in through the normal POST /api/auth/login endpoint like any other user, and
 * its JWT (role=ADMIN) is used to call this endpoint to create TEACHER accounts.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;

    /**
     * Create a teacher account.
     * POST /api/admin/teachers
     * Requires: Authorization: Bearer <admin-jwt>
     *
     * Returns 401 if the caller is not authenticated, 403 if the caller is authenticated
     * but is not an ADMIN.
     */
    @PostMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createTeacher(@RequestBody CreateTeacherRequest request) {
        log.info("Admin request to create teacher account: {}", request.getEmail());
        UserResponse teacher = userService.createTeacher(
                request.getName(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(teacher);
    }
}
