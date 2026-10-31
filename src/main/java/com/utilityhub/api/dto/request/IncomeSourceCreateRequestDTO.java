package com.utilityhub.api.dto.request;

import java.math.BigDecimal;

public record IncomeSourceCreateRequestDTO(
        Integer year,
        String name,
        BigDecimal amount) {
}
