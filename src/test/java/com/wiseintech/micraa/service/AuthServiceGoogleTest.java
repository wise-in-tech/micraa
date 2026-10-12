package com.wiseintech.micraa.service;

import com.wiseintech.micraa.dto.AuthRequest;
import com.wiseintech.micraa.dto.AuthResponse;
import com.wiseintech.micraa.dto.GoogleAuthRequest;
import com.wiseintech.micraa.model.User;
import com.wiseintech.micraa.model.UserRole;
import com.wiseintech.micraa.repository.UserRepository;
import com.wiseintech.micraa.security.GoogleIdentity;
import com.wiseintech.micraa.security.GoogleTokenVerifier;
import com.wiseintech.micraa.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceGoogleTest {

    private static final GoogleIdentity EVE = new GoogleIdentity("google-sub-1", "eve@gmail.com", "Eve");

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtService, passwordEncoder, googleTokenVerifier);
        when(googleTokenVerifier.verify("good-token")).thenReturn(EVE);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            if (u.getId() == null) {
                u.setId(42L);
            }
            return u;
        });
        when(jwtService.generateToken(anyLong(), anyString(), anyString())).thenReturn("app-jwt");
    }

    private AuthResponse login() {
        return authService.loginWithGoogle(new GoogleAuthRequest("good-token"));
    }

    @Test
    void newGoogleUserIsCreatedAsStudentWithoutPassword() {
        when(userRepository.findByGoogleSubject("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("eve@gmail.com")).thenReturn(Optional.empty());

        AuthResponse response = login();

        assertThat(response.getToken()).isEqualTo("app-jwt");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getRole()).isEqualTo("STUDENT");
        assertThat(response.getName()).isEqualTo("Eve");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(saved.getGoogleSubject()).isEqualTo("google-sub-1");
        assertThat(saved.getPasswordHash()).isNull();
    }

    @Test
    void returningGoogleUserIsFoundBySubject() {
        User teacher = User.builder().id(7L).name("Eve").email("eve@gmail.com")
                .googleSubject("google-sub-1").role(UserRole.TEACHER).build();
        when(userRepository.findByGoogleSubject("google-sub-1")).thenReturn(Optional.of(teacher));

        AuthResponse response = login();

        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getRole()).isEqualTo("TEACHER");
        verify(userRepository, never()).save(any());
    }

    @Test
    void existingPasswordAccountIsLinkedByEmailAndKeepsItsRole() {
        User teacher = User.builder().id(7L).name("Eve").email("eve@gmail.com")
                .passwordHash("hash").role(UserRole.TEACHER).build();
        when(userRepository.findByGoogleSubject("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("eve@gmail.com")).thenReturn(Optional.of(teacher));

        AuthResponse response = login();

        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getRole()).isEqualTo("TEACHER");
        assertThat(teacher.getGoogleSubject()).isEqualTo("google-sub-1");
        assertThat(teacher.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void accountLinkedToAnotherGoogleAccountIsRejected() {
        User other = User.builder().id(7L).name("Eve").email("eve@gmail.com")
                .googleSubject("google-sub-OTHER").role(UserRole.STUDENT).build();
        when(userRepository.findByGoogleSubject("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("eve@gmail.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(this::login).isInstanceOf(BadCredentialsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void invalidTokenIsRejected() {
        when(googleTokenVerifier.verify("bad-token")).thenThrow(new BadCredentialsException("Invalid Google ID token"));

        assertThatThrownBy(() -> authService.loginWithGoogle(new GoogleAuthRequest("bad-token")))
                .isInstanceOf(BadCredentialsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void googleOnlyAccountCannotLogInWithPassword() {
        User googleOnly = User.builder().id(7L).name("Eve").email("eve@gmail.com")
                .googleSubject("google-sub-1").role(UserRole.STUDENT).build();
        when(userRepository.findByEmail("eve@gmail.com")).thenReturn(Optional.of(googleOnly));

        assertThatThrownBy(() -> authService.login(
                AuthRequest.builder().email("eve@gmail.com").password("x").build()))
                .hasMessage("Invalid credentials");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
