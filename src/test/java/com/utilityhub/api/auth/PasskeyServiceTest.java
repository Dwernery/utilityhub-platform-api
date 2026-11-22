package com.utilityhub.api.auth;

import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.utilityhub.api.auth.SessionTokenService.IssuedSession;
import com.utilityhub.api.config.ApiSecurityProperties;
import com.utilityhub.api.exception.InvalidRequestException;
import com.utilityhub.api.exception.PasskeyNotSetUpException;
import com.utilityhub.api.exception.SignInFailedException;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorAssertionResponse;
import com.webauthn4j.data.AuthenticatorAttestationResponse;
import com.webauthn4j.data.AuthenticatorSelectionCriteria;
import com.webauthn4j.data.PublicKeyCredential;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.PublicKeyCredentialRpEntity;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import com.webauthn4j.data.ResidentKeyRequirement;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.test.authenticator.webauthn.PackedAuthenticator;
import com.webauthn4j.test.authenticator.webauthn.WebAuthnAuthenticatorAdaptor;
import com.webauthn4j.test.client.ClientPlatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The whole Touch ID flow against a software authenticator: register a passkey, put it in
 * the config (as you would in .env), then sign in with it.
 */
class PasskeyServiceTest {

    private static final String SECRET = "x".repeat(44);
    private static final String ORIGIN = "http://localhost:5173";
    private static final String CLIENT = "10.0.0.1";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final SessionTokenService tokens = new SessionTokenService(SECRET, Duration.ofDays(30), Clock.systemUTC());
    private ClientPlatform touchId;

    @BeforeEach
    void newAuthenticator() {
        touchId = new ClientPlatform(new Origin(ORIGIN), new WebAuthnAuthenticatorAdaptor(new PackedAuthenticator()));
    }

    @Test
    void registerThenSignIn_givesASession() {
        PasskeyService.Registered registered = register(service(List.of()));
        assertThat(registered.envLine()).isEqualTo("PASSKEYS=" + registered.credential());

        PasskeyService configured = service(List.of(registered.credential()));
        PasskeyService.Options options = configured.signInOptions();
        assertThat(options.publicKey())
                .containsEntry("rpId", "localhost")
                .containsEntry("userVerification", "required");

        IssuedSession session = configured.signIn(options.challengeToken(), signInAnswer(options), "MacBook", CLIENT);

        assertThat(session.deviceName()).isEqualTo("MacBook");
        assertThat(tokens.sessionDecoder().decode(session.token()).getClaimAsString("dev"))
                .isEqualTo("MacBook");
    }

    @Test
    void registeringASecondPasskey_listsBothInTheEnvLine() {
        String first = register(service(List.of())).credential();
        newAuthenticator();
        PasskeyService.Registered second = register(service(List.of(first)));

        assertThat(second.envLine()).isEqualTo("PASSKEYS=" + first + "," + second.credential());
    }

    @Test
    void signInWithAPasskeyThatWasRemovedFromConfig_isRefused() {
        ClientPlatform first = touchId;
        String removed = register(service(List.of())).credential();
        newAuthenticator();
        String other = register(service(List.of())).credential();
        touchId = first;

        // The first authenticator answers, but only the second passkey is configured.
        PasskeyService configured = service(List.of(other));
        PasskeyService.Options options = configured.signInOptions();
        String answer = signInAnswerAllowing(options, removed);

        assertThatThrownBy(() -> configured.signIn(options.challengeToken(), answer, "Mac", CLIENT))
                .isInstanceOf(SignInFailedException.class)
                .hasMessageContaining("isn't registered");
    }

    @Test
    void answerForAnotherChallenge_isRefused() {
        PasskeyService configured = service(List.of(register(service(List.of())).credential()));
        PasskeyService.Options first = configured.signInOptions();
        PasskeyService.Options second = configured.signInOptions();

        assertThatThrownBy(() -> configured.signIn(second.challengeToken(), signInAnswer(first), "Mac", CLIENT))
                .isInstanceOf(SignInFailedException.class);
    }

    @Test
    void answerFromAnotherWebsite_isRefused() {
        PasskeyService configured = service(List.of(register(service(List.of())).credential()));
        PasskeyService.Options options = configured.signInOptions();
        touchId.setOrigin(new Origin("https://evil.example"));

        assertThatThrownBy(() -> configured.signIn(options.challengeToken(), signInAnswer(options), "Mac", CLIENT))
                .isInstanceOf(SignInFailedException.class);
    }

    @Test
    void registrationChallenge_cannotBeUsedToSignIn() {
        PasskeyService configured = service(List.of(register(service(List.of())).credential()));
        PasskeyService.Options signIn = configured.signInOptions();
        String registrationToken = configured.registrationOptions().challengeToken();

        assertThatThrownBy(() -> configured.signIn(registrationToken, signInAnswer(signIn), "Mac", CLIENT))
                .isInstanceOf(SignInFailedException.class);
    }

    @Test
    void garbledAnswer_isRefused() {
        PasskeyService configured = service(List.of(register(service(List.of())).credential()));
        String token = configured.signInOptions().challengeToken();

        assertThatThrownBy(() -> configured.signIn(token, "{\"id\":\"nope\"}", "Mac", CLIENT))
                .isInstanceOf(SignInFailedException.class);
        assertThatThrownBy(() ->
                        configured.register(configured.registrationOptions().challengeToken(), "{}"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void noPasskeyConfigured_signInOptionsExplainWhatToDo() {
        assertThatThrownBy(() -> service(List.of()).signInOptions())
                .isInstanceOf(PasskeyNotSetUpException.class)
                .hasMessageContaining("Set up Touch ID");
    }

    @Test
    void passkeysOff_everythingIsUnavailable() {
        PasskeyService off =
                new PasskeyService(properties("", List.of()), tokens, new LoginRateLimiter(Clock.systemUTC()));

        assertThatThrownBy(off::registrationOptions).isInstanceOf(PasskeyNotSetUpException.class);
        assertThatThrownBy(off::signInOptions).isInstanceOf(PasskeyNotSetUpException.class);
    }

    @Test
    void garbledCredentialInConfig_stopsStartup() {
        assertThatThrownBy(() -> service(List.of("not-a-passkey")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("entry 1");
    }

    // --- helpers: what the browser does with the options ---

    private PasskeyService service(List<String> credentials) {
        return new PasskeyService(
                properties("localhost", credentials), tokens, new LoginRateLimiter(Clock.systemUTC()));
    }

    private static ApiSecurityProperties properties(String rpId, List<String> credentials) {
        return new ApiSecurityProperties(
                "$2y$04$I4Whp1UhmuiA1RuMCudLI.tTTkJKQsQpQ2rzAJKJuCdRAiEfbGRl2",
                SECRET,
                Duration.ofDays(30),
                true,
                new ApiSecurityProperties.Passkey(rpId, ORIGIN, credentials),
                List.of());
    }

    private PasskeyService.Registered register(PasskeyService service) {
        PasskeyService.Options options = service.registrationOptions();
        Map<String, Object> publicKey = options.publicKey();
        PublicKeyCredentialCreationOptions creation = new PublicKeyCredentialCreationOptions(
                new PublicKeyCredentialRpEntity("localhost", "UtilityHub"),
                new PublicKeyCredentialUserEntity(B64D.decode(nested(publicKey, "user", "id")), "owner", "owner"),
                new DefaultChallenge(B64D.decode((String) publicKey.get("challenge"))),
                List.of(new PublicKeyCredentialParameters(
                        PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256)),
                120_000L,
                List.of(),
                new AuthenticatorSelectionCriteria(
                        null, ResidentKeyRequirement.PREFERRED, UserVerificationRequirement.REQUIRED),
                AttestationConveyancePreference.NONE,
                null);
        PublicKeyCredential<AuthenticatorAttestationResponse, ?> credential = touchId.create(creation);
        AuthenticatorAttestationResponse response = credential.getResponse();
        // The shape @simplewebauthn/browser sends, extra fields included.
        String json = """
                {"id":"%s","rawId":"%s","type":"public-key","authenticatorAttachment":"platform",
                 "response":{"clientDataJSON":"%s","attestationObject":"%s","transports":["internal"],
                             "publicKeyAlgorithm":-7},
                 "clientExtensionResults":{}}
                """.formatted(
                        B64.encodeToString(credential.getRawId()),
                        B64.encodeToString(credential.getRawId()),
                        B64.encodeToString(response.getClientDataJSON()),
                        B64.encodeToString(response.getAttestationObject()));
        return service.register(options.challengeToken(), json);
    }

    private String signInAnswer(PasskeyService.Options options) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allowed =
                (List<Map<String, Object>>) options.publicKey().get("allowCredentials");
        return answer(
                options,
                allowed.stream().map(c -> B64D.decode((String) c.get("id"))).toList());
    }

    /** Lets the authenticator answer with {@code credential} even if the server didn't list it. */
    private String signInAnswerAllowing(PasskeyService.Options options, String credential) {
        byte[] credentialId = new com.webauthn4j.converter.AttestedCredentialDataConverter(
                        new com.webauthn4j.converter.util.ObjectConverter())
                .extractCredentialId(B64D.decode(credential));
        return answer(options, List.of(credentialId));
    }

    private String answer(PasskeyService.Options options, List<byte[]> allowedIds) {
        PublicKeyCredentialRequestOptions request = new PublicKeyCredentialRequestOptions(
                new DefaultChallenge(B64D.decode((String) options.publicKey().get("challenge"))),
                120_000L,
                "localhost",
                allowedIds.stream()
                        .map(id -> new PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY, id, null))
                        .toList(),
                UserVerificationRequirement.REQUIRED,
                null);
        PublicKeyCredential<AuthenticatorAssertionResponse, ?> credential = touchId.get(request);
        AuthenticatorAssertionResponse response = credential.getResponse();
        return """
                {"id":"%s","rawId":"%s","type":"public-key","authenticatorAttachment":"platform",
                 "response":{"clientDataJSON":"%s","authenticatorData":"%s","signature":"%s","userHandle":"%s"},
                 "clientExtensionResults":{}}
                """.formatted(
                        B64.encodeToString(credential.getRawId()),
                        B64.encodeToString(credential.getRawId()),
                        B64.encodeToString(response.getClientDataJSON()),
                        B64.encodeToString(response.getAuthenticatorData()),
                        B64.encodeToString(response.getSignature()),
                        B64.encodeToString(response.getUserHandle()));
    }

    @SuppressWarnings("unchecked")
    private static String nested(Map<String, Object> map, String key, String inner) {
        return (String) ((Map<String, Object>) map.get(key)).get(inner);
    }
}
