package com.wiseintech.micraa.service;

import com.wiseintech.micraa.dto.AuthRequest;
import com.wiseintech.micraa.dto.AuthResponse;
import com.wiseintech.micraa.model.User;
import com.wiseintech.micraa.model.UserRole;
import com.wiseintech.micraa.repository.UserRepository;
import com.wiseintech.micraa.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ticket-001: "public registration always creates a STUDENT" / "a request containing
 * role=TEACHER (or role=ADMIN) must never create that role".
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    private AuthRequest request(String role) {
        return AuthRequest.builder()
                .name("Eve")
                .email("eve@school.fr")
                .password("password123")
                .role(role)
                .build();
    }

    private void stubHappyPath() {
        authService = new AuthService(userRepository, jwtService, passwordEncoder);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(jwtService.generateToken(anyLong(), anyString(), anyString())).thenReturn("jwt-token");
    }

    @Test
    void registrationWithNoRoleCreatesStudent() {
        stubHappyPath();

        AuthResponse response = authService.register(request(null));

        assertThat(response.getRole()).isEqualTo("STUDENT");
        assertSavedUserRoleIsStudent();
    }

    @Test
    void registrationRequestingTeacherRoleStillCreatesStudent() {
        stubHappyPath();

        AuthResponse response = authService.register(request("TEACHER"));

        assertThat(response.getRole()).isEqualTo("STUDENT");
        assertSavedUserRoleIsStudent();
    }

    @Test
    void registrationRequestingAdminRoleStillCreatesStudent() {
        stubHappyPath();

        AuthResponse response = authService.register(request("ADMIN"));

        assertThat(response.getRole()).isEqualTo("STUDENT");
        assertSavedUserRoleIsStudent();
    }

    private void assertSavedUserRoleIsStudent() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.STUDENT);
    }
}
