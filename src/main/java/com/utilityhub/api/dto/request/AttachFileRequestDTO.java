package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AttachFileRequestDTO(
        @NotBlank(message = "s3Key is required") String s3Key,

        @NotBlank(message = "fileType is required") String fileType) {
}
