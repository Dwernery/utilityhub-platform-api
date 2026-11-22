package com.utilityhub.api.config;

import java.time.Duration;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Typed view of the {@code api.*} properties. Validated at startup, so the app
 * refuses to start without a password hash and signing secret instead of running unprotected.
 *
 * @param passwordHash bcrypt hash of the owner's password. In production it comes from
 *     {@code OWNER_PASSWORD_HASH} in the server's .env; generate it with
 *     {@code scripts/hash-password.sh}.
 * @param sessionSecret signs the session tokens. Changing it signs every device out.
 *     In production it comes from {@code SESSION_SECRET}; generate it with {@code openssl rand -hex 32}.
 * @param sessionTtl how long a device stays signed in without using the app. Each use
 *     past the halfway point hands out a fresh token, so a device in use never expires.
 * @param cookieSecure send the web session cookie only over HTTPS. Off only for local development.
 * @param passkey Touch ID / Face ID sign-in for the web app.
 * @param corsAllowedOrigins browser origins allowed to call the API from another
 *     address. Empty in production: the web UI is served from the same address as the
 *     API, and the mobile app isn't a browser, so neither needs CORS.
 */
@Validated
@ConfigurationProperties(prefix = "api")
public record ApiSecurityProperties(
        @NotBlank
        @Pattern(
                regexp = "\\$2[aby]?\\$\\d{2}\\$[./0-9A-Za-z]{53}",
                message = "must be a bcrypt hash (generate one with scripts/hash-password.sh)")
        String passwordHash,

        @NotBlank @Size(min = 32, message = "must be at least 32 characters (use: openssl rand -hex 32)")
        String sessionSecret,

        @DefaultValue("30d") Duration sessionTtl,

        @DefaultValue("true") boolean cookieSecure,

        @Valid @DefaultValue Passkey passkey,

        @DefaultValue List<String> corsAllowedOrigins) {

    /**
     * @param rpId the site's host name, e.g. {@code utilityhub.example.com}. Passkeys only
     *     work on this host. Blank turns passkeys off.
     * @param origin the address the browser shows, e.g. {@code https://utilityhub.example.com}.
     *     Blank means {@code https://<rpId>}.
     * @param credentials the registered passkeys: each one is the value the "Set up Touch ID"
     *     screen shows. Only public keys, so not secret. Remove one to revoke it.
     */
    public record Passkey(
            @DefaultValue("") String rpId,
            @DefaultValue("") String origin,
            @DefaultValue List<String> credentials) {

        public boolean enabled() {
            return !rpId.isBlank();
        }

        public String effectiveOrigin() {
            return origin.isBlank() ? "https://" + rpId : origin;
        }
    }

    /** Keep the secrets out of logs and error messages. */
    @Override
    public String toString() {
        return "ApiSecurityProperties[passwordHash=<hidden>, sessionSecret=<hidden>, sessionTtl=" + sessionTtl
                + ", cookieSecure=" + cookieSecure + ", passkey=" + passkey + ", corsAllowedOrigins="
                + corsAllowedOrigins + "]";
    }
}
