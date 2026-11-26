package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import com.utilityhub.api.dto.json.WholeNumberDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;

/** Sets how many lessons are done. The apps send the new count for +1 / −1 as well. */
public record CourseProgressRequestDTO(
        @NotNull @PositiveOrZero @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer completedLessons) {}
