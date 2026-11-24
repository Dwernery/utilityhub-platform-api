package com.utilityhub.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * {@code paid} is a {@code Boolean} wrapper rather than a primitive so that a
 * missing value is rejected instead of silently defaulting to {@code false}.
 * {@code cash} is optional: leaving it out keeps the item's current value, so
 * clients that predate editing it can't clear it by accident.
 */
public record TransactionUpdateRequestDTO(
        @NotBlank @Size(max = 100) String name,

        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @NotNull Boolean paid,

        Boolean cash) {}
