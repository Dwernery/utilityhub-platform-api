package com.utilityhub.api.dto.response.library;

public record PresignedUrlResponseDTO(
        String presignedUrl,
        String s3Key) {
}
