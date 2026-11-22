package com.utilityhub.api.config;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.utilityhub.api.auth.SessionTokenService;
import com.utilityhub.api.auth.SessionTokenService.IssuedSession;

/**
 * Keeps a device that is in use signed in. Once a session token is past half its
 * lifetime, the response carries a fresh one: a new cookie for the web app, or the
 * {@value #HEADER} header, which the mobile app saves in place of the old token.
 *
 * <p>Deliberately not a {@code @Component}: Spring Boot would also register it as a
 * plain servlet filter, running it a second time outside Spring Security.
 */
class SessionRefreshFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Session-Token";

    private final SessionTokenService tokens;
    private final SessionCookies cookies;

    SessionRefreshFilter(SessionTokenService tokens, SessionCookies cookies) {
        this.tokens = tokens;
        this.cookies = cookies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken session && tokens.needsRefresh(session.getToken())) {
            Jwt old = session.getToken();
            IssuedSession fresh = tokens.issue(old.getClaimAsString(SessionTokenService.DEVICE_CLAIM));
            if (SessionTokenResolver.cameFromCookie(request)) {
                response.addHeader(HttpHeaders.SET_COOKIE, cookies.create(fresh).toString());
            } else {
                response.setHeader(HEADER, fresh.token());
            }
        }
        chain.doFilter(request, response);
    }
}
