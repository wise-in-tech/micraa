package com.wiseintech.micraa.service;

import com.wiseintech.micraa.config.LiveKitConfig;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveKitService {
    
    private final LiveKitConfig liveKitConfig;
    
    /**
     * Generate a LiveKit access token for a user to join a room
     * 
     * @param roomName The name of the LiveKit room
     * @param participantIdentity The unique identity of the participant
     * @param participantName The display name of the participant
     * @return The JWT access token
     */
    public String generateToken(String roomName, String participantIdentity, String participantName) {
        try {
            log.info("Generating LiveKit token for participant {} in room {}", participantIdentity, roomName);
            
            AccessToken token = new AccessToken(
                liveKitConfig.getApi().getKey(),
                liveKitConfig.getApi().getSecret()
            );
            
            token.setName(participantName);
            token.setIdentity(participantIdentity);
            token.addGrants(new RoomJoin(true), new RoomName(roomName));
            
            // Token valid for 6 hours
            token.setTtl(6 * 60 * 60);
            
            String jwt = token.toJwt();
            log.info("Successfully generated LiveKit token for participant {}", participantIdentity);
            
            return jwt;
        } catch (Exception e) {
            log.error("Failed to generate LiveKit token", e);
            throw new RuntimeException("Failed to generate LiveKit token", e);
        }
    }
    
    /**
     * Get the LiveKit server URL
     */
    public String getLiveKitUrl() {
        return liveKitConfig.getUrl();
    }
}
