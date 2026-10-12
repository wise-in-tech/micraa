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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;

    /**
     * Enregistrer un nouvel utilisateur
     *
     * Public registration always creates a STUDENT account. Any "role" field sent by
     * the client is intentionally ignored: a role supplied in a request body is never
     * trusted as proof of authorization. Teacher accounts can only be created through
     * the authenticated ADMIN-only path (see AdminController / UserService#createTeacher).
     */
    public AuthResponse register(AuthRequest request) {
        // Vérifier si l'email existe déjà
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.STUDENT)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {} ({})", user.getEmail(), user.getId());

        return toAuthResponse(user);
    }

    /**
     * Connecter un utilisateur existant
     */
    public AuthResponse login(AuthRequest request) {
        // Trouver l'utilisateur par email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Vérifier le password (un compte créé via Google n'a pas de mot de passe local)
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for user: {}", request.getEmail());
            throw new RuntimeException("Invalid credentials");
        }

        log.info("User logged in: {} ({})", user.getEmail(), user.getId());

        return toAuthResponse(user);
    }

    /**
     * Connexion / inscription via Google Sign-In.
     *
     * The Flutter app sends the Google ID token; once verified, the user is found by
     * Google subject, else linked by verified email to an existing account (role kept),
     * else created as a STUDENT - same rule as public registration.
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        if (request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new IllegalArgumentException("idToken is required");
        }

        GoogleIdentity identity = googleTokenVerifier.verify(request.getIdToken());

        User user = userRepository.findByGoogleSubject(identity.subject())
                .orElseGet(() -> linkOrCreateGoogleUser(identity));

        log.info("User logged in with Google: {} ({})", user.getEmail(), user.getId());
        return toAuthResponse(user);
    }

    private User linkOrCreateGoogleUser(GoogleIdentity identity) {
        User existing = userRepository.findByEmail(identity.email()).orElse(null);

        if (existing != null) {
            // Already linked to another Google account: never silently re-link.
            if (existing.getGoogleSubject() != null) {
                log.warn("Google sub {} tried to use {} already linked to another Google account",
                        identity.subject(), identity.email());
                throw new BadCredentialsException("Account is linked to a different Google account");
            }
            existing.setGoogleSubject(identity.subject());
            log.info("Linked Google account to existing user {} ({})", existing.getEmail(), existing.getId());
            return userRepository.save(existing);
        }

        String name = identity.name() == null || identity.name().isBlank() ? identity.email() : identity.name();
        User created = userRepository.save(User.builder()
                .name(name)
                .email(identity.email())
                .googleSubject(identity.subject())
                .role(UserRole.STUDENT)
                .build());
        log.info("User registered with Google: {} ({})", created.getEmail(), created.getId());
        return created;
    }

    private AuthResponse toAuthResponse(User user) {
        // Générer le token JWT
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }
}
