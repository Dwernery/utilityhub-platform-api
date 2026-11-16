package com.utilityhub.api.config;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Typed view of the {@code api.*} properties. Validated at startup, so the app
 * refuses to start without a proper token instead of running unprotected.
 *
 * @param token the shared secret every request must send as {@code Authorization: Bearer <token>}.
 *     In production it comes from the {@code API_TOKEN} environment variable; generate one
 *     with {@code openssl rand -base64 32}.
 * @param corsAllowedOrigins browser origins allowed to call the API from another
 *     address. Empty in production: the web UI is served from the same address as the
 *     API, and the mobile app isn't a browser, so neither needs CORS.
 */
@Validated
@ConfigurationProperties(prefix = "api")
public record ApiSecurityProperties(
        @NotBlank @Size(min = 32, message = "must be at least 32 characters (use: openssl rand -base64 32)")
        String token,

        @DefaultValue List<String> corsAllowedOrigins) {

    /** Keep the token out of logs and error messages. */
    @Override
    public String toString() {
        return "ApiSecurityProperties[token=<hidden>, corsAllowedOrigins=" + corsAllowedOrigins + "]";
    }
}
