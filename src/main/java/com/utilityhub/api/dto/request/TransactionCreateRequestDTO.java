package com.utilityhub.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.utilityhub.api.db.entity.finance.TransactionType;

/**
 * {@code paid} and {@code cash} are optional and default to {@code false}: a new
 * item is normally unpaid, and most items aren't cash.
 */
public record TransactionCreateRequestDTO(
        @NotBlank @Size(max = 100) String name,
        @NotNull TransactionType transactionType,

        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        Boolean paid,
        Boolean cash) {}
