package com.utilityhub.api.dto.response.auth;

/**
 * @param credential the new passkey's public key and id, to add to PASSKEYS in the server's .env
 * @param envLine the complete line, with every passkey already registered plus this one
 */
public record PasskeyRegisteredResponseDTO(String credential, String envLine) {}
