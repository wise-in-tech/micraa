package com.wiseintech.micraa.controller;

import com.wiseintech.micraa.dto.AuthRequest;
import com.wiseintech.micraa.dto.AuthResponse;
import com.wiseintech.micraa.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Enregistrer un nouvel utilisateur
     * POST /api/auth/register
     * {
     *   "email": "student@school.fr",
     *   "password": "password123",
     *   "name": "Jean Dupont",
     *   "role": "STUDENT"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Se connecter
     * POST /api/auth/login
     * {
     *   "email": "student@school.fr",
     *   "password": "password123"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
