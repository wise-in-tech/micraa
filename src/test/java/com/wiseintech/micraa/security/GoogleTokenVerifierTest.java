package com.wiseintech.micraa.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Signs tokens with a locally generated RSA key standing in for Google's, so every
 * check (signature, iss, aud, exp, email_verified) is exercised without network access.
 */
class GoogleTokenVerifierTest {

    private static final String CLIENT_ID = "our-client.apps.googleusercontent.com";

    private static RSAKey googleKey;
    private static RSAKey attackerKey;

    @BeforeAll
    static void keys() throws Exception {
        googleKey = new RSAKeyGenerator(2048).keyID("google").generate();
        attackerKey = new RSAKeyGenerator(2048).keyID("google").generate();
    }

    private GoogleTokenVerifier verifier(List<String> clientIds) throws Exception {
        return new GoogleTokenVerifier(NimbusJwtDecoder.withPublicKey(googleKey.toRSAPublicKey()).build(), clientIds);
    }

    private String token(RSAKey key, Consumer<JWTClaimsSet.Builder> customize) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(CLIENT_ID)
                .subject("1234567890")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .claim("email", "eve@gmail.com")
                .claim("email_verified", true)
                .claim("name", "Eve");
        customize.accept(claims);

        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims.build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    @Test
    void validTokenReturnsIdentity() throws Exception {
        GoogleIdentity identity = verifier(List.of(CLIENT_ID)).verify(token(googleKey, c -> {}));

        assertThat(identity).isEqualTo(new GoogleIdentity("1234567890", "eve@gmail.com", "Eve"));
    }

    @Test
    void issuerWithoutSchemeIsAccepted() throws Exception {
        String token = token(googleKey, c -> c.issuer("accounts.google.com"));

        assertThat(verifier(List.of(CLIENT_ID)).verify(token).subject()).isEqualTo("1234567890");
    }

    @Test
    void stringEmailVerifiedIsAccepted() throws Exception {
        String token = token(googleKey, c -> c.claim("email_verified", "true"));

        assertThat(verifier(List.of(CLIENT_ID)).verify(token).email()).isEqualTo("eve@gmail.com");
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        String token = token(attackerKey, c -> {});

        assertThatThrownBy(() -> verifier(List.of(CLIENT_ID)).verify(token))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void tokenForAnotherAppIsRejected() throws Exception {
        String token = token(googleKey, c -> c.audience("someone-else.apps.googleusercontent.com"));

        assertThatThrownBy(() -> verifier(List.of(CLIENT_ID)).verify(token))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() throws Exception {
        String token = token(googleKey, c -> c.issuer("https://evil.example.com"));

        assertThatThrownBy(() -> verifier(List.of(CLIENT_ID)).verify(token))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Instant past = Instant.now().minusSeconds(7200);
        String token = token(googleKey, c -> c.issueTime(Date.from(past)).expirationTime(Date.from(past.plusSeconds(3600))));

        assertThatThrownBy(() -> verifier(List.of(CLIENT_ID)).verify(token))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void unverifiedEmailIsRejected() throws Exception {
        String token = token(googleKey, c -> c.claim("email_verified", false));

        assertThatThrownBy(() -> verifier(List.of(CLIENT_ID)).verify(token))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void malformedTokenIsRejected() throws Exception {
        assertThatThrownBy(() -> verifier(List.of(CLIENT_ID)).verify("not-a-jwt"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void noConfiguredClientIdRejectsEverything() throws Exception {
        String token = token(googleKey, c -> {});

        assertThatThrownBy(() -> verifier(List.of()).verify(token))
                .isInstanceOf(BadCredentialsException.class);
    }
}
