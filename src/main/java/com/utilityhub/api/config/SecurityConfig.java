package com.utilityhub.api.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import tools.jackson.databind.json.JsonMapper;

/**
 * Who may call the API: anyone holding the API token (see {@link ApiTokenAuthenticationFilter}).
 * Everything else gets a 401 in the same RFC 9457 problem-detail format as other errors.
 */
@Configuration
@EnableConfigurationProperties(ApiSecurityProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ApiSecurityProperties properties, JsonMapper jsonMapper)
            throws Exception {
        AuthenticationEntryPoint unauthorized = problemDetailEntryPoint(jsonMapper);

        http
                // CSRF attacks ride on credentials the browser attaches by itself (cookies).
                // This API has none: the token is sent explicitly by our own code.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
                // Every request carries the token, so nothing is remembered between requests.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Spring Boot's error page, and the API docs (only reachable on the
                        // server itself: nginx forwards nothing but /api/). The docs list the
                        // endpoints but return no data; calling an endpoint still needs the token.
                        .requestMatchers("/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized))
                .addFilterBefore(
                        new ApiTokenAuthenticationFilter(properties.token(), unauthorized),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static CorsConfigurationSource corsConfigurationSource(ApiSecurityProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.corsAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        // No cookies are involved, so the browser never needs to send credentials.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 401 as problem detail, plus the {@code WWW-Authenticate} challenge from RFC 6750:
     * plain {@code Bearer} when no token was sent, {@code error="invalid_token"} for a wrong one.
     */
    private static AuthenticationEntryPoint problemDetailEntryPoint(JsonMapper jsonMapper) {
        return (request, response, exception) -> {
            boolean wrongToken = exception instanceof BadCredentialsException;

            Map<String, Object> problem = new LinkedHashMap<>();
            problem.put("type", "about:blank");
            problem.put("title", HttpStatus.UNAUTHORIZED.getReasonPhrase());
            problem.put("status", HttpStatus.UNAUTHORIZED.value());
            problem.put("detail", wrongToken ? "The API token is not valid." : "An API token is required.");
            problem.put("instance", request.getRequestURI());

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, wrongToken ? "Bearer error=\"invalid_token\"" : "Bearer");
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(jsonMapper.writeValueAsString(problem));
        };
    }
}
