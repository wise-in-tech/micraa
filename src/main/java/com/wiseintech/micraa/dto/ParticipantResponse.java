package com.wiseintech.micraa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A connected participant as reported live by LiveKit, enriched with the resolved
 * application user id and role. Deliberately excludes tokens/secrets - only what the
 * teacher UI needs to render the roster and moderate microphones.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantResponse {
    private String identity;
    private Long userId;
    private String name;
    private String role; // TEACHER | STUDENT | UNKNOWN
    private long connectedAt; // epoch millis, from LiveKit's own joinedAt timestamp
    private String state; // LiveKit ParticipantInfo.State: JOINING | JOINED | ACTIVE | DISCONNECTED
    private boolean microphoneMuted;
}
