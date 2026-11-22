package com.utilityhub.api.controller;

import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utilityhub.api.auth.AuthService;
import com.utilityhub.api.auth.PasskeyService;
import com.utilityhub.api.auth.SessionTokenService;
import com.utilityhub.api.auth.SessionTokenService.IssuedSession;
import com.utilityhub.api.config.SessionCookies;
import com.utilityhub.api.dto.request.auth.PasskeyLoginRequestDTO;
import com.utilityhub.api.dto.request.auth.PasskeyRegisterRequestDTO;
import com.utilityhub.api.dto.request.auth.PasswordLoginRequestDTO;
import com.utilityhub.api.dto.response.auth.PasskeyOptionsResponseDTO;
import com.utilityhub.api.dto.response.auth.PasskeyRegisteredResponseDTO;
import com.utilityhub.api.dto.response.auth.SessionResponseDTO;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Signing in and out. A successful sign-in starts a session for that device:
 *
 * <ul>
 *   <li>the web app gets an HttpOnly {@code uh_session} cookie;
 *   <li>a client that sends {@code X-Client: mobile} gets the token in the response body
 *       instead, and sends it back as {@code Authorization: Bearer <token>}.
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Sign in with the password or a passkey, sign out")
public class AuthController {

    /** Request header that asks for the session token in the body rather than a cookie. */
    public static final String CLIENT_HEADER = "X-Client";

    private static final String MOBILE = "mobile";

    private final AuthService authService;
    private final PasskeyService passkeyService;
    private final SessionCookies cookies;

    public AuthController(AuthService authService, PasskeyService passkeyService, SessionCookies cookies) {
        this.authService = authService;
        this.passkeyService = passkeyService;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    public ResponseEntity<SessionResponseDTO> login(
            @Valid @RequestBody PasswordLoginRequestDTO request,
            @RequestHeader(name = CLIENT_HEADER, required = false) String client,
            HttpServletRequest http) {
        return started(
                authService.signInWithPassword(request.password(), request.deviceName(), http.getRemoteAddr()), client);
    }

    /** Clears the web app's cookie. The mobile app just forgets its token. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }

    /** The current session; the web app calls it on load to find out whether it is signed in. */
    @GetMapping("/me")
    public SessionResponseDTO me(@AuthenticationPrincipal Jwt session) {
        Instant expiresAt = session.getExpiresAt();
        return new SessionResponseDTO(session.getClaimAsString(SessionTokenService.DEVICE_CLAIM), expiresAt, null);
    }

    @PostMapping("/passkey/options")
    public PasskeyOptionsResponseDTO passkeySignInOptions() {
        PasskeyService.Options options = passkeyService.signInOptions();
        return new PasskeyOptionsResponseDTO(options.challengeToken(), options.publicKey());
    }

    @PostMapping("/passkey/login")
    public ResponseEntity<SessionResponseDTO> passkeyLogin(
            @Valid @RequestBody PasskeyLoginRequestDTO request,
            @RequestHeader(name = CLIENT_HEADER, required = false) String client,
            HttpServletRequest http) {
        return started(
                passkeyService.signIn(
                        request.challengeToken(),
                        request.credential().toString(),
                        request.deviceName(),
                        http.getRemoteAddr()),
                client);
    }

    /** Step 1 of "Set up Touch ID" (needs a session: you sign in with the password first). */
    @PostMapping("/passkey/register/options")
    public PasskeyOptionsResponseDTO passkeyRegistrationOptions() {
        PasskeyService.Options options = passkeyService.registrationOptions();
        return new PasskeyOptionsResponseDTO(options.challengeToken(), options.publicKey());
    }

    /** Step 2: checks the new passkey and returns the line to add to the server's .env. */
    @PostMapping("/passkey/register")
    public PasskeyRegisteredResponseDTO registerPasskey(@Valid @RequestBody PasskeyRegisterRequestDTO request) {
        PasskeyService.Registered registered = passkeyService.register(
                request.challengeToken(), request.credential().toString());
        return new PasskeyRegisteredResponseDTO(registered.credential(), registered.envLine());
    }

    private ResponseEntity<SessionResponseDTO> started(IssuedSession session, String client) {
        if (MOBILE.equalsIgnoreCase(client)) {
            return ResponseEntity.ok(
                    new SessionResponseDTO(session.deviceName(), session.expiresAt(), session.token()));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookies.create(session).toString())
                .body(new SessionResponseDTO(session.deviceName(), session.expiresAt(), null));
    }
}
