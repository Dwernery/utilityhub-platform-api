package com.utilityhub.api.dto.response.finance;

import java.math.BigDecimal;

/** One account's balance within a net-worth history entry. */
public record AccountBalanceResponseDTO(
        Integer accountId, String accountName, String accountType, String category, BigDecimal balance) {}
