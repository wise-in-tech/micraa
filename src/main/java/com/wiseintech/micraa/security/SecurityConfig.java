package com.wiseintech.micraa.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Cost factor 12 for extra security
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Activer CORS
                .cors(cors -> {})
                
                // Désactiver CSRF pour API REST (nous utilisons JWT)
                .csrf(csrf -> csrf.disable())
                
                // Politique de session sans état (stateless)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Pas d'authentification anonyme implicite : une requête sans JWT valide
                // doit être vue comme "non authentifiée" (401), pas comme un principal
                // anonyme qui échouerait la vérification de rôle (403). Voir
                // exceptionHandling ci-dessous pour la distinction 401 / 403.
                .anonymous(anonymous -> anonymous.disable())

                // Autorisation des endpoints
                .authorizeHttpRequests(authorize -> authorize
                        // Routes publiques (pas besoin de JWT)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/health").permitAll()

                        // Toutes les autres routes nécessitent l'authentification
                        .anyRequest().authenticated()
                )

                // 401 pour un appelant non authentifié (pas de JWT / JWT invalide),
                // 403 pour un appelant authentifié sans le rôle requis.
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonError(response, 401, "Authentication required"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, 403, "Access denied"))
                )

                // Ajouter le filtre JWT
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJsonError(jakarta.servlet.http.HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // Fixed, controlled message text only (no user input interpolated here), so a
        // minimal hand-built JSON object avoids depending on a specific Jackson major
        // version's package layout for this one error payload.
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
