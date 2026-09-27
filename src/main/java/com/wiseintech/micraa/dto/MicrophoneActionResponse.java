package com.wiseintech.micraa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a teacher mute/unmute action on a student's microphone.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MicrophoneActionResponse {
    private Long participantId;
    private boolean muted;
}
