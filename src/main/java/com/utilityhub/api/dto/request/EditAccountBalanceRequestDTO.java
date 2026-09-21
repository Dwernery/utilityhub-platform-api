package com.utilityhub.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EditAccountBalanceRequestDTO(
        Integer accountId,
        LocalDate balanceDate,
        BigDecimal balance) {
}
