package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorCreateRequestDTO(
        @NotBlank(message = "Author name is required") @Size(max = 255)
        String fullName) {}
