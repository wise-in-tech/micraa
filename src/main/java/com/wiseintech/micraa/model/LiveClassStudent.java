package com.wiseintech.micraa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "live_class_students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(LiveClassStudent.LiveClassStudentId.class)
public class LiveClassStudent {
    
    @Id
    @Column(name = "live_class_id")
    private Long liveClassId;
    
    @Id
    @Column(name = "student_id")
    private Long studentId;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveClassStudentId implements Serializable {
        private Long liveClassId;
        private Long studentId;
    }
}
