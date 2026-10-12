package com.wiseintech.micraa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Sign-In configuration.
 *
 * clientIds = OAuth client IDs accepted as the "aud" claim of a Google ID token.
 * With google_sign_in on Android the token's audience is the *Web* client ID passed
 * as serverClientId; on iOS it is the iOS client ID. List every ID the app uses.
 * Env var: GOOGLE_AUTH_CLIENT_IDS (comma-separated). Empty = Google login disabled.
 */
@Configuration
@ConfigurationProperties(prefix = "google.auth")
@Data
public class GoogleAuthConfig {
    private List<String> clientIds = new ArrayList<>();
}
