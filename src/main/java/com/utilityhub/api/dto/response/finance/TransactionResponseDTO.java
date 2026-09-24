package com.utilityhub.api.dto.response.finance;

import java.math.BigDecimal;
import com.utilityhub.api.db.entity.finance.TransactionType;

public record TransactionResponseDTO(
        Integer id,
        String name,
        TransactionType transactionType,
        BigDecimal amount,
        boolean paid,
        boolean cash
) {
}