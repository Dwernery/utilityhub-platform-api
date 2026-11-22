package com.utilityhub.api.config;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/** The app must refuse to start rather than run with a missing password hash or a weak secret. */
class ApiSecurityPropertiesTest {

    private static final String HASH = "api.password-hash=$2y$04$I4Whp1UhmuiA1RuMCudLI.tTTkJKQsQpQ2rzAJKJuCdRAiEfbGRl2";
    private static final String SECRET = "api.session-secret=" + "x".repeat(44);

    @Configuration
    @EnableConfigurationProperties(ApiSecurityProperties.class)
    static class Bind {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Bind.class);

    @Test
    void nothingConfigured_stopsStartup() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void blankPasswordHash_stopsStartup() {
        // What docker compose would pass if OWNER_PASSWORD_HASH were empty in the server's .env.
        runner.withPropertyValues("api.password-hash=", SECRET)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void plainPasswordInsteadOfHash_stopsStartup_withAHelpfulMessage() {
        runner.withPropertyValues("api.password-hash=hunter2", SECRET)
                .run(context ->
                        assertThat(context).getFailure().rootCause().hasMessageContaining("scripts/hash-password.sh"));
    }

    @Test
    void shortSecret_stopsStartup_withAHelpfulMessage() {
        runner.withPropertyValues(HASH, "api.session-secret=password123")
                .run(context ->
                        assertThat(context).getFailure().rootCause().hasMessageContaining("openssl rand -hex 32"));
    }

    @Test
    void validSettings_start_withSafeDefaults() {
        runner.withPropertyValues(HASH, SECRET).run(context -> {
            assertThat(context).hasNotFailed();
            ApiSecurityProperties properties = context.getBean(ApiSecurityProperties.class);
            assertThat(properties.sessionTtl()).isEqualTo(Duration.ofDays(30));
            assertThat(properties.cookieSecure()).isTrue();
            assertThat(properties.passkey().enabled()).isFalse();
            assertThat(properties.passkey().credentials()).isEmpty();
            assertThat(properties.corsAllowedOrigins()).isEmpty();
        });
    }

    @Test
    void passkeyOrigin_defaultsToHttpsOnTheRpId() {
        runner.withPropertyValues(HASH, SECRET, "api.passkey.rp-id=hub.example.com", "api.passkey.credentials=a,b")
                .run(context -> {
                    ApiSecurityProperties.Passkey passkey =
                            context.getBean(ApiSecurityProperties.class).passkey();
                    assertThat(passkey.enabled()).isTrue();
                    assertThat(passkey.effectiveOrigin()).isEqualTo("https://hub.example.com");
                    assertThat(passkey.credentials()).containsExactly("a", "b");
                });
    }

    @Test
    void toString_neverIncludesTheSecrets() {
        String hash = "$2y$04$I4Whp1UhmuiA1RuMCudLI.tTTkJKQsQpQ2rzAJKJuCdRAiEfbGRl2";
        String secret = "s3cret-".repeat(6);
        String text = new ApiSecurityProperties(
                        hash,
                        secret,
                        Duration.ofDays(30),
                        true,
                        new ApiSecurityProperties.Passkey("", "", List.of()),
                        List.of())
                .toString();
        assertThat(text).doesNotContain(hash).doesNotContain(secret);
    }
}
