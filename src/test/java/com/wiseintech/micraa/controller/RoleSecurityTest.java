package com.wiseintech.micraa.controller;

import com.wiseintech.micraa.model.User;
import com.wiseintech.micraa.model.UserRole;
import com.wiseintech.micraa.repository.UserRepository;
import com.wiseintech.micraa.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage (real Spring Security filter chain + H2 database) for ticket-001's
 * account/role rules:
 * - public registration always creates a STUDENT, whatever role is requested;
 * - only an authenticated ADMIN can create a TEACHER, via POST /api/admin/teachers;
 * - unauthenticated calls get 401, authenticated-but-unauthorized calls get 403;
 * - the legacy public "POST /api/users" teacher-creation path no longer exists.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String tokenFor(String email, UserRole role) {
        User user = User.builder()
                .name(email)
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .role(role)
                .build();
        user = userRepository.save(user);
        return jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    // ─── Public registration always creates a STUDENT ──────────────────────

    @Test
    void registerIgnoresClientSuppliedTeacherRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Teacher?","email":"wannabe-teacher@school.fr",
                                 "password":"password123","role":"TEACHER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("STUDENT")));

        assertThat(userRepository.findByEmail("wannabe-teacher@school.fr"))
                .get().extracting(User::getRole).isEqualTo(UserRole.STUDENT);
    }

    @Test
    void registerIgnoresClientSuppliedAdminRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Admin?","email":"wannabe-admin@school.fr",
                                 "password":"password123","role":"ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("STUDENT")));
    }

    // ─── Teacher account creation: ADMIN-only ──────────────────────────────

    @Test
    void unauthenticatedCallerCannotCreateTeacher() throws Exception {
        mockMvc.perform(post("/api/admin/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Prof","email":"prof1@school.fr","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized());

        assertThat(userRepository.findByEmail("prof1@school.fr")).isEmpty();
    }

    @Test
    void studentCannotCreateTeacher() throws Exception {
        String studentToken = tokenFor("student1@school.fr", UserRole.STUDENT);

        mockMvc.perform(post("/api/admin/teachers")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Prof","email":"prof2@school.fr","password":"password123"}
                                """))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findByEmail("prof2@school.fr")).isEmpty();
    }

    @Test
    void teacherCannotCreateAnotherTeacher() throws Exception {
        String teacherToken = tokenFor("teacher1@school.fr", UserRole.TEACHER);

        mockMvc.perform(post("/api/admin/teachers")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Prof","email":"prof3@school.fr","password":"password123"}
                                """))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findByEmail("prof3@school.fr")).isEmpty();
    }

    @Test
    void adminCanCreateTeacher() throws Exception {
        String adminToken = tokenFor("admin1@school.fr", UserRole.ADMIN);

        mockMvc.perform(post("/api/admin/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Prof Martin","email":"prof4@school.fr","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("TEACHER")));

        assertThat(userRepository.findByEmail("prof4@school.fr"))
                .get().extracting(User::getRole).isEqualTo(UserRole.TEACHER);
    }

    // ─── Legacy public "POST /api/users" path is gone ──────────────────────

    @Test
    void legacyPublicUserCreationEndpointNoLongerExists() throws Exception {
        String teacherToken = tokenFor("teacher2@school.fr", UserRole.TEACHER);

        // "/api/users" still exists for GET (listing), but POST is no longer mapped to
        // anything -> 405, definitely not a successful user/role creation.
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Prof","email":"prof5@school.fr","role":"TEACHER"}
                                """))
                .andExpect(status().isMethodNotAllowed());

        assertThat(userRepository.findByEmail("prof5@school.fr")).isEmpty();
    }

    @Test
    void legacyEndpointUnauthenticatedGetsAuthenticationChallengeNotData() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }
}
