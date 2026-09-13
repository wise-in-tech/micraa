package com.wiseintech.micraa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long liveClassId;
    
    @Column(nullable = false)
    private Long studentId;
    
    @Column(nullable = false)
    private LocalDateTime joinedAt;
    
    private LocalDateTime leftAt;
}
