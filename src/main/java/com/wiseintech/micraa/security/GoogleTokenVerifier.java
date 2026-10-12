package com.wiseintech.micraa.security;

import com.wiseintech.micraa.config.GoogleAuthConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verifies a Google ID token obtained on-device by the Flutter google_sign_in plugin.
 *
 * Checks: RS256 signature against Google's public keys (fetched and cached by Nimbus),
 * exp/nbf, iss = accounts.google.com, aud = one of our client IDs, email_verified = true.
 * Fails closed: if no client ID is configured, every token is rejected.
 */
@Component
@Slf4j
public class GoogleTokenVerifier {

    static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final NimbusJwtDecoder decoder;
    private final boolean enabled;

    @Autowired
    public GoogleTokenVerifier(GoogleAuthConfig config) {
        this(NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build(), config.getClientIds());
    }

    /** Visible for tests: lets a test sign tokens with its own key. */
    GoogleTokenVerifier(NimbusJwtDecoder decoder, List<String> clientIds) {
        Set<String> allowedAudiences = clientIds == null ? Set.of() : clientIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());

        this.enabled = !allowedAudiences.isEmpty();
        if (!enabled) {
            log.warn("Google Sign-In disabled: google.auth.client-ids (GOOGLE_AUTH_CLIENT_IDS) is not set");
        }

        decoder.setJwtValidator(validator(allowedAudiences));
        this.decoder = decoder;
    }

    private static OAuth2TokenValidator<Jwt> validator(Set<String> allowedAudiences) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<String>(JwtClaimNames.ISS, GOOGLE_ISSUERS::contains),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.stream().anyMatch(allowedAudiences::contains))
        );
    }

    public GoogleIdentity verify(String idToken) {
        if (!enabled) {
            throw new BadCredentialsException("Google Sign-In is not configured");
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            // Details (expired, wrong audience, bad signature...) are logged, not returned.
            log.warn("Rejected Google ID token: {}", e.getMessage());
            throw new BadCredentialsException("Invalid Google ID token");
        }

        String email = jwt.getClaimAsString("email");
        // email_verified is a JSON boolean in current tokens, but was a string in older ones.
        Object emailVerified = jwt.getClaims().get("email_verified");
        boolean verified = Boolean.TRUE.equals(emailVerified) || "true".equals(emailVerified);
        if (email == null || email.isBlank() || !verified) {
            log.warn("Rejected Google ID token for sub {}: email missing or not verified", jwt.getSubject());
            throw new BadCredentialsException("Google account email is not verified");
        }

        return new GoogleIdentity(jwt.getSubject(), email, jwt.getClaimAsString("name"));
    }
}
