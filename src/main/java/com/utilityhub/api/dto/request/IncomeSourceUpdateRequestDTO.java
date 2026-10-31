package com.utilityhub.api.dto.request;

import java.math.BigDecimal;

public record IncomeSourceUpdateRequestDTO(
        Integer id,
        String name,
        BigDecimal amount) {
}
