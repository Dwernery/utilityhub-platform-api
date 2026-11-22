package com.utilityhub.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.utilityhub.api.auth.SessionTokenService.IssuedSession;
import com.utilityhub.api.config.ApiSecurityProperties;
import com.utilityhub.api.exception.InvalidRequestException;
import com.utilityhub.api.exception.PasskeyNotSetUpException;
import com.utilityhub.api.exception.SignInFailedException;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;

/**
 * Touch ID / Face ID sign-in with passkeys (WebAuthn), without a database.
 *
 * <ul>
 *   <li>A registered passkey is just its public key (plus its id). The "Set up Touch ID"
 *       step hands it back as text, and you add it to {@code PASSKEYS} in the server's .env,
 *       which ends up in {@code api.passkey.credentials}. Removing it there revokes it.
 *   <li>The random challenge each ceremony needs travels to the browser and back inside a
 *       signed, two-minute token (see {@link SessionTokenService#issueChallenge}), instead
 *       of being remembered by the server.
 *   <li>The signature counter is not tracked: synced passkeys (iCloud Keychain) always
 *       report 0, and tracking it would need somewhere to store it.
 * </ul>
 */
@Service
public class PasskeyService {

    static final String SIGN_IN = "passkey-sign-in";
    static final String REGISTER = "passkey-register";
    static final long TIMEOUT_MILLIS = 120_000;

    /** There is one user, so every passkey belongs to this fixed id. */
    private static final byte[] USER_ID = "utility-hub-owner".getBytes(StandardCharsets.UTF_8);

    private static final List<PublicKeyCredentialParameters> ALGORITHMS = List.of(
            new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
            new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256));

    private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();

    private final ApiSecurityProperties.Passkey config;
    private final SessionTokenService tokens;
    private final LoginRateLimiter rateLimiter;
    private final List<StoredPasskey> passkeys;
    private final WebAuthnManager webAuthn = WebAuthnManager.createNonStrictWebAuthnManager();
    private final SecureRandom random = new SecureRandom();

    /** One entry of {@code api.passkey.credentials}: as written there, and decoded. */
    private record StoredPasskey(String value, AttestedCredentialData data) {}

    /** What the browser needs to start a ceremony, plus the signed challenge to send back with its answer. */
    public record Options(String challengeToken, Map<String, Object> publicKey) {}

    /** A newly registered passkey, ready to be added to the server's .env. */
    public record Registered(String credential, String envLine) {}

    public PasskeyService(ApiSecurityProperties properties, SessionTokenService tokens, LoginRateLimiter rateLimiter) {
        this.config = properties.passkey();
        this.tokens = tokens;
        this.rateLimiter = rateLimiter;
        this.passkeys = decode(config.credentials());
    }

    public Options signInOptions() {
        requireEnabled();
        if (passkeys.isEmpty()) {
            throw new PasskeyNotSetUpException(
                    "Touch ID isn't set up yet. Sign in with your password, then choose \"Set up Touch ID\".");
        }
        byte[] challenge = newChallenge();
        Map<String, Object> publicKey = new LinkedHashMap<>();
        publicKey.put("challenge", BASE64URL.encodeToString(challenge));
        publicKey.put("rpId", config.rpId());
        publicKey.put("allowCredentials", credentialDescriptors());
        publicKey.put("userVerification", "required");
        publicKey.put("timeout", TIMEOUT_MILLIS);
        return new Options(tokens.issueChallenge(SIGN_IN, challenge), publicKey);
    }

    /**
     * @param credentialJson the browser's answer, as {@code PublicKeyCredential.toJSON()} gives it
     * @param client the caller's IP address, for rate limiting
     * @throws SignInFailedException the answer doesn't check out
     */
    public IssuedSession signIn(String challengeToken, String credentialJson, String deviceName, String client) {
        requireEnabled();
        rateLimiter.checkAllowed(client);
        try {
            byte[] challenge = tokens.readChallenge(challengeToken, SIGN_IN);
            AuthenticationData answer = webAuthn.parseAuthenticationResponseJSON(credentialJson);
            StoredPasskey passkey = passkeys.stream()
                    .filter(p -> Arrays.equals(p.data().getCredentialId(), answer.getCredentialId()))
                    .findFirst()
                    .orElseThrow(() -> new SignInFailedException(
                            "This passkey isn't registered on the server. Set up Touch ID again."));

            CredentialRecord record = new CredentialRecordImpl(
                    new NoneAttestationStatement(), null, null, null, 0L, passkey.data(), null, null, null, null);
            webAuthn.verify(
                    answer,
                    new AuthenticationParameters(
                            serverProperty(challenge),
                            record,
                            passkeys.stream()
                                    .map(p -> p.data().getCredentialId())
                                    .toList(),
                            true,
                            true));
        } catch (SignInFailedException e) {
            rateLimiter.recordFailure(client);
            throw e;
        } catch (RuntimeException e) {
            // Bad or expired challenge, unreadable answer (DataConversionException, or a
            // missing field deep inside the parser), failed check (VerificationException).
            rateLimiter.recordFailure(client);
            throw new SignInFailedException("Touch ID sign-in could not be verified. Try again.");
        }
        rateLimiter.recordSuccess(client);
        return tokens.issue(AuthService.cleanDeviceName(deviceName));
    }

    public Options registrationOptions() {
        requireEnabled();
        byte[] challenge = newChallenge();
        Map<String, Object> publicKey = new LinkedHashMap<>();
        publicKey.put("rp", Map.of("id", config.rpId(), "name", "UtilityHub"));
        publicKey.put(
                "user",
                Map.of("id", BASE64URL.encodeToString(USER_ID), "name", "owner", "displayName", "UtilityHub owner"));
        publicKey.put("challenge", BASE64URL.encodeToString(challenge));
        publicKey.put(
                "pubKeyCredParams",
                ALGORITHMS.stream()
                        .map(a -> Map.of("type", "public-key", "alg", a.getAlg().getValue()))
                        .toList());
        publicKey.put("timeout", TIMEOUT_MILLIS);
        // Don't let the same device register twice.
        publicKey.put("excludeCredentials", credentialDescriptors());
        publicKey.put(
                "authenticatorSelection",
                Map.of(
                        // Touch ID on this computer, not a phone or a security key.
                        "authenticatorAttachment", "platform",
                        "residentKey", "preferred",
                        "userVerification", "required"));
        publicKey.put("attestation", "none");
        return new Options(tokens.issueChallenge(REGISTER, challenge), publicKey);
    }

    /** Checks a new passkey and returns it as text for the server's .env. Nothing is stored here. */
    public Registered register(String challengeToken, String credentialJson) {
        requireEnabled();
        try {
            byte[] challenge = tokens.readChallenge(challengeToken, REGISTER);
            RegistrationData data = webAuthn.parseRegistrationResponseJSON(credentialJson);
            webAuthn.verify(data, new RegistrationParameters(serverProperty(challenge), ALGORITHMS, true, true));
            AttestedCredentialData credential =
                    data.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
            String value = BASE64URL.encodeToString(converter().convert(credential));
            String all = Stream.concat(passkeys.stream().map(StoredPasskey::value), Stream.of(value))
                    .collect(Collectors.joining(","));
            return new Registered(value, "PASSKEYS=" + all);
        } catch (RuntimeException e) {
            // Same kinds of failure as in signIn.
            throw new InvalidRequestException("The new passkey could not be verified. Try again.");
        }
    }

    private void requireEnabled() {
        if (!config.enabled()) {
            throw new PasskeyNotSetUpException(
                    "Passkeys are not enabled on this server (api.passkey.rp-id is not set).");
        }
    }

    private ServerProperty serverProperty(byte[] challenge) {
        return new ServerProperty(
                Origin.create(config.effectiveOrigin()), config.rpId(), new DefaultChallenge(challenge));
    }

    private List<Map<String, Object>> credentialDescriptors() {
        return passkeys.stream()
                .map(p -> Map.<String, Object>of(
                        "type",
                        "public-key",
                        "id",
                        BASE64URL.encodeToString(p.data().getCredentialId())))
                .toList();
    }

    private byte[] newChallenge() {
        byte[] challenge = new byte[32];
        random.nextBytes(challenge);
        return challenge;
    }

    private static AttestedCredentialDataConverter converter() {
        return new AttestedCredentialDataConverter(new ObjectConverter());
    }

    /** Fails startup on a garbled entry, rather than silently ignoring a passkey. */
    private static List<StoredPasskey> decode(List<String> values) {
        List<StoredPasskey> decoded = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i).strip();
            if (value.isEmpty() || !seen.add(value)) continue;
            try {
                decoded.add(new StoredPasskey(
                        value, converter().convert(Base64.getUrlDecoder().decode(value))));
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                        "api.passkey.credentials entry " + (i + 1) + " is not a valid passkey"
                                + " (copy it again from \"Set up Touch ID\")",
                        e);
            }
        }
        return List.copyOf(decoded);
    }
}
