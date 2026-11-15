package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SeriesCreateRequestDTO(
        @NotBlank(message = "Series name is required") @Size(max = 255)
        String name) {}
