package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create or rename a group. */
public record CourseGroupRequestDTO(
        @NotBlank(message = "Group name is required") @Size(max = 255)
        String name) {}
