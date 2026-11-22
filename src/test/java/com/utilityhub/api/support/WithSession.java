package com.utilityhub.api.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.utilityhub.api.auth.SessionTokenService;
import com.utilityhub.api.config.SecurityConfig;

/**
 * For controller tests: applies the real security rules, and sends a valid session
 * token on every request, so tests can focus on what the endpoint does.
 *
 * <p>Use {@code @Import(WithSession.class)} on a {@code @WebMvcTest}. The rules
 * themselves are tested without this, in {@code SessionAuthenticationTest}.
 */
@TestConfiguration
@Import(SecurityConfig.class)
public class WithSession {

    @Bean
    MockMvcBuilderCustomizer sendSessionOnEveryRequest(SessionTokenService tokens) {
        String token = tokens.issue("Test device").token();
        return builder -> builder.defaultRequest(
                MockMvcRequestBuilders.get("/").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
