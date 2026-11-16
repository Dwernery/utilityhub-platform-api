package com.utilityhub.api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Checks {@code Authorization: Bearer <token>} against the configured API token.
 *
 * <ul>
 *   <li>Right token: the request is marked authenticated and continues.
 *   <li>Wrong token: rejected here with a 401 ({@code error="invalid_token"}).
 *   <li>No Bearer token at all: the request continues unauthenticated, and the
 *       authorization rules in {@link SecurityConfig} turn it into a 401.
 * </ul>
 *
 * <p>Deliberately not a {@code @Component}: Spring Boot would also register it as a
 * plain servlet filter, running it a second time outside Spring Security.
 */
class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final byte[] expectedTokenHash;
    private final AuthenticationEntryPoint entryPoint;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    ApiTokenAuthenticationFilter(String expectedToken, AuthenticationEntryPoint entryPoint) {
        this.expectedTokenHash = sha256(expectedToken);
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        // Scheme names are case-insensitive (RFC 9110), so "bearer x" counts too.
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            chain.doFilter(request, response);
            return;
        }

        if (!matchesExpectedToken(header.substring(BEARER.length()).trim())) {
            contextHolder.clearContext();
            entryPoint.commence(request, response, new BadCredentialsException("Invalid API token"));
            return;
        }

        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("api-client", null, List.of()));
        contextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    /**
     * Compares SHA-256 hashes in constant time. A plain {@code equals} stops at the
     * first wrong character, so response timing could reveal the token one character
     * at a time; hashing first also hides the token's length.
     */
    private boolean matchesExpectedToken(String presented) {
        return MessageDigest.isEqual(expectedTokenHash, sha256(presented));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available on the JVM", e);
        }
    }
}
