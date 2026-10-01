package com.utilityhub.api.dto.request;

import java.time.LocalDate;

public record DeleteAccountBalanceRequestDTO(
        Integer accountId,
        LocalDate balanceDate) {
}
