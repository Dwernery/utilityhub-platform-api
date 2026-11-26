package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.utilityhub.api.dto.json.WholeNumberDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * A course's editable fields, for create (POST) and full update (PUT). The creator must
 * already exist (add it with POST /api/courses/creators first), as with a book's author.
 */
public record CourseRequestDTO(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String creatorName,
        /** Null for no group. */
        Integer groupId,

        @NotNull @Positive @Max(10000) @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer totalLessons,
        /** Defaults to 0 when left out. */
        @PositiveOrZero @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer completedLessons,

        @Size(max = 2048) // Empty means "no link", like null.
        @Pattern(regexp = "^$|^https?://\\S+$", message = "must be a web address (http:// or https://)")
        String url) {

    @JsonIgnore
    @AssertTrue(message = "completedLessons cannot exceed totalLessons")
    public boolean isProgressWithinTotal() {
        return completedLessons == null || totalLessons == null || completedLessons <= totalLessons;
    }
}
