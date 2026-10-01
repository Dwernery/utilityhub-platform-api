package com.utilityhub.api.dto.request;

import java.math.BigDecimal;

public record EditTransactionRequestDTO(
        Integer id,
        String name,
        BigDecimal amount,
        boolean paid) {
}
