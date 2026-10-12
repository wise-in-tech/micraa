package com.wiseintech.micraa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    // Null pour un compte créé via Google Sign-In (pas de mot de passe local).
    // Base existante : ddl-auto=update ne retire pas un NOT NULL, voir doc/tickets/ticket-003.md.
    @Column
    private String passwordHash;

    // Identifiant Google stable ("sub" de l'ID token), null si jamais connecté via Google.
    @Column(unique = true)
    private String googleSubject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
