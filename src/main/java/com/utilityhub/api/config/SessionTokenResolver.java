package com.utilityhub.api.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.WebUtils;

/**
 * Where a request's session token comes from: {@code Authorization: Bearer <token>} (the
 * mobile app, curl) or else the {@code uh_session} cookie (the web app).
 *
 * <p>The sign-in and sign-out endpoints never look for one: an expired cookie would
 * otherwise get a 401 there and make it impossible to sign in again.
 */
class SessionTokenResolver implements BearerTokenResolver {

    /** Set on requests authenticated by the cookie, for the CSRF check and token refresh. */
    static final String FROM_COOKIE = SessionTokenResolver.class.getName() + ".fromCookie";

    private final BearerTokenResolver header = new DefaultBearerTokenResolver();
    private final RequestMatcher ignored;

    SessionTokenResolver(RequestMatcher ignored) {
        this.ignored = ignored;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        if (ignored.matches(request)) return null;

        String token = header.resolve(request);
        if (token != null) return token;

        Cookie cookie = WebUtils.getCookie(request, SessionCookies.NAME);
        if (cookie == null || cookie.getValue().isBlank()) return null;
        request.setAttribute(FROM_COOKIE, Boolean.TRUE);
        return cookie.getValue();
    }

    static boolean cameFromCookie(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(FROM_COOKIE));
    }
}
