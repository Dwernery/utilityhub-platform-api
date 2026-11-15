package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BookCreateRequestDTO(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Positive Integer pages,
        @NotBlank @Size(max = 255) String authorName,
        @Size(max = 255) String seriesName,

        @NotNull @Pattern(regexp = "\\d{13}", message = "must be exactly 13 digits")
        String isbn13) {}
