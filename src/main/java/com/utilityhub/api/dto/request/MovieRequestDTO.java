package com.utilityhub.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of POST and PUT /api/mcu/movies. A special (e.g. "Werewolf by Night") is a
 * movie with {@code special = true}. Watch status is not part of this: it only
 * changes through PUT /tracker/{globalId}/status.
 *
 * @param domain "Phase 1"–"Phase 6" files it under a saga; any other name is an
 *     Expanded Universe category
 */
public record MovieRequestDTO(
        @NotBlank @Size(max = 255) String title,
        @NotNull LocalDate premiereDate,
        @NotNull @Positive @Max(1000) Integer runtime,
        @NotBlank String synopsis,
        @NotBlank @Size(max = 255) String domain,
        @NotNull Boolean special) {}
