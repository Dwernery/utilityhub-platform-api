package com.utilityhub.api.dto.response.courses;

import java.time.Instant;

/** A course as the apps see it. {@code groupId} is null for an ungrouped course. */
public record CourseResponseDTO(
        Integer id,
        String title,
        String creatorName,
        Integer groupId,
        Integer totalLessons,
        Integer completedLessons,
        String url,
        String s3Url,
        Instant createdAt) {}
