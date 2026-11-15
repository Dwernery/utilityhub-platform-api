package com.utilityhub.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** Body of PUT /accounts/{accountId}/balances/{balanceDate}; the URL identifies which balance. */
public record AccountBalanceUpdateRequestDTO(
        @NotNull @Digits(integer = 13, fraction = 2) BigDecimal balance) {}
