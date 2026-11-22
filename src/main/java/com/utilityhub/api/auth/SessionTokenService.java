package com.utilityhub.api.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Creates and checks the signed tokens that stand in for a database of sessions.
 *
 * <p>A session token is an HS256 JWT signed with {@code api.session-secret}. It holds the
 * device name and an expiry, nothing else, so the server keeps no state: a token is
 * valid exactly when its signature checks out and it hasn't expired. The catch: one
 * token can't be revoked on its own. Changing the secret revokes all of them.
 *
 * <p>The same key also signs the short-lived passkey challenges. The {@code aud} claim
 * keeps the kinds apart, so a challenge can never be used as a session.
 */
public class SessionTokenService {

    public static final String DEVICE_CLAIM = "dev";

    static final String ISSUER = "utility-hub";
    static final String SESSION_AUDIENCE = "session";
    static final Duration CHALLENGE_TTL = Duration.ofMinutes(2);

    private static final String CHALLENGE_CLAIM = "chl";

    private final JwtEncoder encoder;
    private final NimbusJwtDecoder sessionDecoder;
    private final NimbusJwtDecoder challengeDecoder;
    private final Duration sessionTtl;
    private final Clock clock;

    /** A token just handed out, and what it says. */
    public record IssuedSession(String token, String deviceName, Instant expiresAt) {}

    public SessionTokenService(String secret, Duration sessionTtl, Clock clock) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(key).build();
        this.sessionDecoder = decoder(
                key,
                clock,
                new JwtClaimValidator<List<String>>(
                        JwtClaimNames.AUD, aud -> aud != null && aud.contains(SESSION_AUDIENCE)));
        this.challengeDecoder = decoder(key, clock, null);
        this.sessionTtl = sessionTtl;
        this.clock = clock;
    }

    /** Checks session tokens for Spring Security's resource-server filter. */
    public JwtDecoder sessionDecoder() {
        return sessionDecoder;
    }

    public IssuedSession issue(String deviceName) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(sessionTtl);
        String token = encode(JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject("owner")
                .audience(List.of(SESSION_AUDIENCE))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(DEVICE_CLAIM, deviceName)
                .build());
        return new IssuedSession(token, deviceName, expiresAt);
    }

    /**
     * True once more than half of the token's lifetime has passed. The caller then hands
     * out a fresh token, so a device that keeps using the app stays signed in.
     */
    public boolean needsRefresh(Jwt session) {
        Instant issuedAt = session.getIssuedAt();
        return issuedAt == null || clock.instant().isAfter(issuedAt.plus(sessionTtl.dividedBy(2)));
    }

    /**
     * Wraps a WebAuthn challenge so the server doesn't have to remember it until the
     * browser answers. {@code purpose} ties it to one step (sign-in or registration).
     */
    String issueChallenge(String purpose, byte[] challenge) {
        Instant now = clock.instant();
        return encode(JwtClaimsSet.builder()
                .issuer(ISSUER)
                .audience(List.of(purpose))
                .issuedAt(now)
                .expiresAt(now.plus(CHALLENGE_TTL))
                .claim(CHALLENGE_CLAIM, Base64.getUrlEncoder().withoutPadding().encodeToString(challenge))
                .build());
    }

    /** The challenge inside a token from {@link #issueChallenge}, if it is genuine, unexpired and for {@code purpose}. */
    byte[] readChallenge(String token, String purpose) {
        try {
            Jwt jwt = challengeDecoder.decode(token);
            if (jwt.getAudience() == null || !jwt.getAudience().contains(purpose)) {
                throw new BadCredentialsException("Challenge is for another step");
            }
            return Base64.getUrlDecoder().decode(jwt.getClaimAsString(CHALLENGE_CLAIM));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new BadCredentialsException("Invalid or expired challenge", e);
        }
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static NimbusJwtDecoder decoder(SecretKey key, Clock clock, OAuth2TokenValidator<Jwt> extra) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // The same server issues and checks the tokens, so no allowance for clock drift.
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        timestamps.setAllowEmptyExpiryClaim(false);
        OAuth2TokenValidator<Jwt> issuer = new JwtIssuerValidator(ISSUER);
        decoder.setJwtValidator(
                extra == null
                        ? new DelegatingOAuth2TokenValidator<>(timestamps, issuer)
                        : new DelegatingOAuth2TokenValidator<>(timestamps, issuer, extra));
        return decoder;
    }
}
