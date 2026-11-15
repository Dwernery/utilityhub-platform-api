package com.utilityhub.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.utilityhub.api.db.entity.finance.AccountCategory;

public record AccountCreateRequestDTO(
        @NotBlank @Size(max = 50) String accountName,
        @NotBlank @Size(max = 50) String accountType,
        @NotNull AccountCategory category,
        @Digits(integer = 13, fraction = 2) BigDecimal balance,
        LocalDate balanceDate) {

    /** Cross-field rule: an initial balance needs a date, and vice versa. */
    @AssertTrue(message = "balance and balanceDate must be provided together")
    public boolean isInitialBalanceComplete() {
        return (balance == null) == (balanceDate == null);
    }
}
