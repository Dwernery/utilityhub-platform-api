package com.utilityhub.api.dto.response.finance;

import java.math.BigDecimal;

public record IncomeSourceResponseDTO(Integer id, Integer year, String name, BigDecimal amount) {}
