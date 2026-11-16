package com.utilityhub.api.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.utilityhub.api.config.SecurityConfig;

/**
 * For controller tests: applies the real security rules, and sends the test API
 * token on every request, so tests can focus on what the endpoint does.
 *
 * <p>Use {@code @Import(WithApiToken.class)} on a {@code @WebMvcTest}. The rules
 * themselves are tested without this, in {@code ApiTokenAuthenticationTest}.
 */
@TestConfiguration
@Import(SecurityConfig.class)
public class WithApiToken {

    @Bean
    MockMvcBuilderCustomizer sendApiTokenOnEveryRequest(@Value("${api.token}") String token) {
        return builder -> builder.defaultRequest(
                MockMvcRequestBuilders.get("/").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
