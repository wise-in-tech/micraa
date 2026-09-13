package com.wiseintech.micraa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRequest {
    private String email;
    private String password;
    private String name;        // Utilisé uniquement pour register
    private String role;        // Utilisé uniquement pour register (TEACHER ou STUDENT)
}
