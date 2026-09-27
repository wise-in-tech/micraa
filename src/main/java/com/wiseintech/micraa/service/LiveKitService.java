package com.wiseintech.micraa.service;

import com.wiseintech.micraa.config.LiveKitConfig;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveKitService {

    private final LiveKitConfig liveKitConfig;

    // Lazily created: RoomServiceClient talks to LiveKit's HTTP admin API (listing
    // participants, forcing a mute) as opposed to AccessToken which only signs JWTs
    // locally. Spring Boot never routes audio itself - this is authorization/control
    // plane only, LiveKit still owns real-time media transport.
    private volatile RoomServiceClient roomServiceClient;
    
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

    /**
     * List the participants currently connected to a LiveKit room, as reported live by
     * LiveKit itself (identity, display name, joinedAt timestamp, connection state,
     * published tracks). Returns an empty list if the room does not exist (e.g. nobody
     * has joined it yet) instead of throwing, since "no participants" is a normal state.
     */
    public List<LivekitModels.ParticipantInfo> listParticipants(String roomName) {
        try {
            Response<List<LivekitModels.ParticipantInfo>> response =
                    getRoomServiceClient().listParticipants(roomName).execute();

            if (!response.isSuccessful() || response.body() == null) {
                log.warn("LiveKit listParticipants({}) returned HTTP {}", roomName, response.code());
                return Collections.emptyList();
            }

            return response.body();
        } catch (IOException e) {
            log.error("Failed to reach LiveKit to list participants for room {}", roomName, e);
            throw new RuntimeException("Failed to reach LiveKit server", e);
        }
    }

    /**
     * Fetch a single participant's current LiveKit state (used to locate their published
     * microphone track before muting/unmuting it). Returns empty if the participant is
     * not currently connected to the room.
     */
    public Optional<LivekitModels.ParticipantInfo> getParticipant(String roomName, String participantIdentity) {
        try {
            Response<LivekitModels.ParticipantInfo> response =
                    getRoomServiceClient().getParticipant(roomName, participantIdentity).execute();

            if (!response.isSuccessful() || response.body() == null) {
                return Optional.empty();
            }

            return Optional.of(response.body());
        } catch (IOException e) {
            log.error("Failed to reach LiveKit to fetch participant {} in room {}", participantIdentity, roomName, e);
            throw new RuntimeException("Failed to reach LiveKit server", e);
        }
    }

    /**
     * Force-mute or force-unmute a participant's published track using LiveKit's
     * server-side admin API. This is enforced by LiveKit itself (the SFU), not merely a
     * hint to the client - a participant's own client cannot silently override it.
     */
    public void setTrackMuted(String roomName, String participantIdentity, String trackSid, boolean muted) {
        try {
            Response<LivekitModels.TrackInfo> response =
                    getRoomServiceClient().mutePublishedTrack(roomName, participantIdentity, trackSid, muted).execute();

            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        "LiveKit refused to " + (muted ? "mute" : "unmute") + " track: HTTP " + response.code());
            }

            log.info("LiveKit track {} of participant {} in room {} set muted={}",
                    trackSid, participantIdentity, roomName, muted);
        } catch (IOException e) {
            log.error("Failed to reach LiveKit to set mute state for participant {} in room {}",
                    participantIdentity, roomName, e);
            throw new RuntimeException("Failed to reach LiveKit server", e);
        }
    }

    private RoomServiceClient getRoomServiceClient() {
        RoomServiceClient client = roomServiceClient;
        if (client == null) {
            synchronized (this) {
                client = roomServiceClient;
                if (client == null) {
                    client = RoomServiceClient.create(
                            resolveApiHost(),
                            liveKitConfig.getApi().getKey(),
                            liveKitConfig.getApi().getSecret());
                    roomServiceClient = client;
                }
            }
        }
        return client;
    }

    /**
     * RoomServiceClient talks to LiveKit's HTTP admin API and expects an http(s) host,
     * while livekit.url is configured as the ws(s) URL used by media clients. Normalize
     * the scheme so both configurations can share the single livekit.url property.
     */
    private String resolveApiHost() {
        String url = liveKitConfig.getUrl();
        if (url.startsWith("wss://")) {
            return "https://" + url.substring("wss://".length());
        }
        if (url.startsWith("ws://")) {
            return "http://" + url.substring("ws://".length());
        }
        return url;
    }
}
