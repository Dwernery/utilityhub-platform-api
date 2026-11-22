package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** @param s3Key the key returned by GET /api/mcu/poster-upload-url */
public record PosterAttachRequestDTO(
        @NotBlank
                @Pattern(regexp = "mcu-posters/[^/]+", message = "must be a key from /api/mcu/poster-upload-url")
                String s3Key) {}
