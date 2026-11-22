package com.utilityhub.api.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import tools.jackson.databind.JsonNode;

/**
 * Body of POST /api/auth/passkey/register.
 *
 * @param challengeToken as returned by POST /api/auth/passkey/register/options
 * @param credential the browser's answer, {@code PublicKeyCredential.toJSON()}
 */
public record PasskeyRegisterRequestDTO(
        @NotBlank @Size(max = 2000) String challengeToken,
        @NotNull JsonNode credential) {}
