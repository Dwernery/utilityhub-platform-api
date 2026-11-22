package com.utilityhub.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of POST and PUT /api/mcu/shows. Each show row is one season, so "Loki"
 * season 2 is its own row with its own synopsis, premiere date and poster.
 */
public record ShowSeasonRequestDTO(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Positive @Max(100) Integer season,
        @NotNull LocalDate premiereDate,
        @NotBlank String synopsis,
        @NotBlank @Size(max = 255) String domain) {}
