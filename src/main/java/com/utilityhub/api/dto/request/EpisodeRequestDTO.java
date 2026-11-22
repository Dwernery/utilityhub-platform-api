package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Body of POST /shows/{showId}/episodes and PUT /episodes/{id}. */
public record EpisodeRequestDTO(
        @NotNull @Positive @Max(1000) Integer episodeNumber,
        @NotBlank @Size(max = 255) String title,
        @NotNull @Positive @Max(1000) Integer runtime) {}
