package com.utilityhub.api.config;

import java.time.Clock;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.utilityhub.api.auth.LoginRateLimiter;
import com.utilityhub.api.auth.SessionTokenService;
import com.utilityhub.api.controller.AuthController;

import tools.jackson.databind.json.JsonMapper;

/**
 * Who may call the API: anyone with a valid session token, which the sign-in endpoints
 * hand out (see {@code AuthController}). The token arrives as {@code Authorization: Bearer}
 * from the mobile app, or as the {@code uh_session} cookie from the web app.
 * Everything else gets a 401 in the same RFC 9457 problem-detail format as other errors.
 */
@Configuration
@EnableConfigurationProperties(ApiSecurityProperties.class)
public class SecurityConfig {

    /** Reachable without a session: signing in, and signing out (which only clears the cookie). */
    static final RequestMatcher SIGN_IN_ENDPOINTS = new OrRequestMatcher(
            post("/api/auth/login"),
            post("/api/auth/logout"),
            post("/api/auth/passkey/options"),
            post("/api/auth/passkey/login"));

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            ApiSecurityProperties properties,
            SessionTokenService tokens,
            SessionCookies cookies,
            JsonMapper jsonMapper)
            throws Exception {
        AuthenticationEntryPoint unauthorized = problemDetailEntryPoint(jsonMapper);

        http
                // Spring's CSRF tokens are replaced by CookieOriginFilter, which suits a
                // JSON API whose only cookie is the session cookie.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
                // Every request carries its token, so nothing is remembered between requests.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.requestMatchers(SIGN_IN_ENDPOINTS)
                        .permitAll()
                        // Spring Boot's error page, and the API docs (only reachable on the
                        // server itself: nginx forwards nothing but /api/). The docs list the
                        // endpoints but return no data; calling an endpoint still needs a session.
                        .requestMatchers("/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(server -> server.bearerTokenResolver(new SessionTokenResolver(SIGN_IN_ENDPOINTS))
                        .jwt(jwt -> jwt.decoder(tokens.sessionDecoder()))
                        .authenticationEntryPoint(unauthorized))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized))
                .addFilterBefore(
                        new CookieOriginFilter(properties.corsAllowedOrigins(), jsonMapper),
                        BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new SessionRefreshFilter(tokens, cookies), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SessionTokenService sessionTokenService(ApiSecurityProperties properties, Clock clock) {
        return new SessionTokenService(properties.sessionSecret(), properties.sessionTtl(), clock);
    }

    @Bean
    SessionCookies sessionCookies(ApiSecurityProperties properties) {
        return new SessionCookies(properties.cookieSecure());
    }

    @Bean
    LoginRateLimiter loginRateLimiter(Clock clock) {
        return new LoginRateLimiter(clock);
    }

    private static CorsConfigurationSource corsConfigurationSource(ApiSecurityProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.corsAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(
                List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, AuthController.CLIENT_HEADER));
        config.setExposedHeaders(List.of(SessionRefreshFilter.HEADER));
        // The web app is served from the API's own address, so the session cookie never
        // has to cross origins.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 401 as problem detail, plus the {@code WWW-Authenticate} challenge from RFC 6750:
     * plain {@code Bearer} when no token was sent, {@code error="invalid_token"} for a bad or expired one.
     */
    private static AuthenticationEntryPoint problemDetailEntryPoint(JsonMapper jsonMapper) {
        return (request, response, exception) -> {
            boolean badToken = isBadToken(exception);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, badToken ? "Bearer error=\"invalid_token\"" : "Bearer");
            ProblemResponses.write(
                    response,
                    jsonMapper,
                    request,
                    HttpStatus.UNAUTHORIZED,
                    badToken ? "Your session has expired. Please sign in again." : "Please sign in.");
        };
    }

    /** Anything but "no credentials at all" means a token was sent and rejected. */
    private static boolean isBadToken(AuthenticationException exception) {
        return !(exception instanceof org.springframework.security.authentication.InsufficientAuthenticationException);
    }

    private static RequestMatcher post(String path) {
        return PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, path);
    }
}
