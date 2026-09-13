package com.wiseintech.micraa.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (jwtService.validateToken(token)) {
                    Long userId = jwtService.extractUserId(token);
                    String email = jwtService.extractEmail(token);
                    String role = jwtService.extractRole(token);

                    if (userId != null) {
                        // Créer une authentification
                        // Le rôle est préfixé avec "ROLE_" pour Spring Security
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        email,
                                        null,
                                        java.util.List.of(
                                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role)
                                        )
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // Ajouter l'ID utilisateur en tant qu'attribut pour utilisation dans les controllers
                        request.setAttribute("userId", userId);
                        request.setAttribute("userEmail", email);
                        request.setAttribute("userRole", role);

                        log.debug("JWT validated for user: {} ({})", email, userId);
                    }
                } else {
                    log.debug("JWT validation failed for token");
                }
            }
        } catch (Exception e) {
            log.error("JWT filter error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
