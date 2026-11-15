package com.utilityhub.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record IncomeSourceCreateRequestDTO(
        @NotNull @Min(1900) @Max(2100) Integer year,
        @NotBlank @Size(max = 100) String name,

        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2)
        BigDecimal amount) {}
