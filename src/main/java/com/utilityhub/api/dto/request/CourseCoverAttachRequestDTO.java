package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/** The key that GET /api/courses/{id}/cover-upload-url handed out, once the upload finished. */
public record CourseCoverAttachRequestDTO(
        @NotBlank(message = "s3Key is required") String s3Key) {}
