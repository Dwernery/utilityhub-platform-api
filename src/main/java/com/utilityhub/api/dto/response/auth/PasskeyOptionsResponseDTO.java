package com.utilityhub.api.dto.response.auth;

import java.util.Map;

/**
 * @param challengeToken send back unchanged with the browser's answer
 * @param publicKey options for {@code navigator.credentials.get/create}, in their JSON form
 */
public record PasskeyOptionsResponseDTO(String challengeToken, Map<String, Object> publicKey) {}
