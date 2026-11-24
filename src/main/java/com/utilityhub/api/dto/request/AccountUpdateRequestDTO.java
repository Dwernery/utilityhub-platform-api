package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Replaces an account's name, type and archived flag. The category can't be
 * changed: the type must belong to the account's current category, because
 * turning an asset into a liability would rewrite every past month's net worth.
 * {@code archived} is a {@code Boolean} wrapper so a missing value is rejected
 * instead of silently defaulting to {@code false}.
 */
public record AccountUpdateRequestDTO(
        @NotBlank @Size(max = 50) String accountName,
        @NotBlank @Size(max = 50) String accountType,
        @NotNull Boolean archived) {}
