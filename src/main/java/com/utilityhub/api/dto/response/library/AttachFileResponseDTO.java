package com.utilityhub.api.dto.response.library;

public record AttachFileResponseDTO(
        boolean success,
        String fileUrl,
        String message) {
}
