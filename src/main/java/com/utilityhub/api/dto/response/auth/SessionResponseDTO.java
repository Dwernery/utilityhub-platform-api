package com.utilityhub.api.dto.response.auth;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The signed-in session. {@code token} is only filled in for the mobile app, right after
 * signing in: it keeps the token in the Keychain. The web app gets a cookie instead and
 * never sees the token.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionResponseDTO(String deviceName, Instant expiresAt, String token) {}
