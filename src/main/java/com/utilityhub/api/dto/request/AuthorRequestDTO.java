package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AuthorRequestDTO(
        @NotBlank(message = "Author name is required")
        String fullName
){}
