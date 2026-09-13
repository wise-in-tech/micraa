package com.wiseintech.micraa.dto;

import com.wiseintech.micraa.model.LiveClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveClassResponse {
    private Long id;
    private String title;
    private Long teacherId;
    private String teacherName;
    private LiveClassStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
}
