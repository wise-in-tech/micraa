package com.wiseintech.micraa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveClass {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private Long teacherId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LiveClassStatus status;
    
    private LocalDateTime scheduledAt;
    
    private LocalDateTime startedAt;
    
    private LocalDateTime endedAt;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = LiveClassStatus.SCHEDULED;
        }
    }
}
