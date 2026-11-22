package com.utilityhub.api.config;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.ResponseCookie;

import com.utilityhub.api.auth.SessionTokenService.IssuedSession;

/**
 * The web app's session cookie. JavaScript can't read it ({@code HttpOnly}), so a script
 * injected into the page can't steal the session, and the browser only sends it to this
 * site's API ({@code SameSite=Strict}, {@code Path=/api}).
 */
public class SessionCookies {

    public static final String NAME = "uh_session";

    private static final String PATH = "/api";

    private final boolean secure;

    public SessionCookies(boolean secure) {
        this.secure = secure;
    }

    public ResponseCookie create(IssuedSession session) {
        Duration maxAge = Duration.between(Instant.now(), session.expiresAt());
        return base(session.token())
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);
    }
}
