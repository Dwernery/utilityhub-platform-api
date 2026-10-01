package com.utilityhub.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountCreateRequestDTO(
        String accountName,
        String accountType,
        String category,
        @JsonProperty(required = false) BigDecimal balance,
        @JsonProperty(required = false) LocalDate balanceDate) {
}
