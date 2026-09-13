package com.wiseintech.micraa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLiveClassRequest {
    private String title;
    private Long teacherId;
    private String scheduledAt; // ISO format
}
