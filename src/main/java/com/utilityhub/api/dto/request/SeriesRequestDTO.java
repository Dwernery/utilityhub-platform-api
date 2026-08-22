package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SeriesRequestDTO(
        @NotBlank(message = "Series name is required") 
        String name
){}

