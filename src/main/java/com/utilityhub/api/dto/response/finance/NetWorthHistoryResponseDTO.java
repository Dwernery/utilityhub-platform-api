package com.utilityhub.api.dto.response.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One point on the net-worth chart.
 *
 * @param source   "historical" (a recorded total) or "account-based" (summed from balances)
 * @param accounts the per-account breakdown; null for historical entries
 */
public record NetWorthHistoryResponseDTO(
        LocalDate date, String source, BigDecimal netWorth, List<AccountBalanceResponseDTO> accounts) {}
