package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** The number of books to finish in the year named in the URL. */
public record ReadingGoalRequestDTO(
        @NotNull @Positive @Max(1000) Integer target) {}
