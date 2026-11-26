package com.utilityhub.api.dto.response.courses;

/** Where to PUT the image, and the key to send to attach-cover afterwards. */
public record CourseCoverUploadResponseDTO(String presignedUrl, String s3Key) {}
