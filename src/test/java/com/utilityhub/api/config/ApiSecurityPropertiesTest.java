package com.utilityhub.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/** The app must refuse to start rather than run with a missing or weak token. */
class ApiSecurityPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(ApiSecurityProperties.class)
    static class Bind {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Bind.class);

    @Test
    void missingToken_stopsStartup() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void blankToken_stopsStartup() {
        // What docker compose passes when API_TOKEN is missing from the server's .env.
        runner.withPropertyValues("api.token=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shortToken_stopsStartup_withAHelpfulMessage() {
        runner.withPropertyValues("api.token=password123")
                .run(context ->
                        assertThat(context).getFailure().rootCause().hasMessageContaining("openssl rand -hex 32"));
    }

    @Test
    void longToken_starts_andCorsDefaultsToNoOrigins() {
        runner.withPropertyValues("api.token=" + "x".repeat(44)).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ApiSecurityProperties.class).corsAllowedOrigins())
                    .isEmpty();
        });
    }

    @Test
    void toString_neverIncludesTheToken() {
        String token = "s3cret-".repeat(6);
        assertThat(new ApiSecurityProperties(token, java.util.List.of()).toString())
                .doesNotContain(token);
    }
}
