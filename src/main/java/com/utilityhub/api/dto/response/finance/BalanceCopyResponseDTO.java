package com.utilityhub.api.dto.response.finance;

import java.time.LocalDate;

/**
 * Result of copying one month's account balances into the next.
 * {@code copied} counts only the balances created: accounts that already had a
 * balance in {@code toDate} are left as they were.
 */
public record BalanceCopyResponseDTO(LocalDate fromDate, LocalDate toDate, int copied) {}
