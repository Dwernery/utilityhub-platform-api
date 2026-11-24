package com.utilityhub.api.service;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.utilityhub.api.db.repository.finance.AccountMonthlyBalanceRepository;
import com.utilityhub.api.db.repository.finance.AccountRepository;
import com.utilityhub.api.db.repository.finance.AccountTypeRepository;
import com.utilityhub.api.db.repository.finance.IncomeSourceRepository;
import com.utilityhub.api.db.repository.finance.NetWorthSnapshotRepository;
import com.utilityhub.api.db.repository.finance.TransactionRepository;
import com.utilityhub.api.dto.response.finance.BalanceCopyResponseDTO;
import com.utilityhub.api.exception.InvalidRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Plain unit test with mocked repositories: the rules around copying a month's balances. */
class FinanceServiceCopyBalancesTest {

    private AccountMonthlyBalanceRepository balances;
    private FinanceService service;

    @BeforeEach
    void setUp() {
        balances = mock(AccountMonthlyBalanceRepository.class);
        service = new FinanceService(
                mock(NetWorthSnapshotRepository.class),
                balances,
                mock(TransactionRepository.class),
                mock(AccountRepository.class),
                mock(AccountTypeRepository.class),
                mock(IncomeSourceRepository.class));
    }

    @Test
    void copiesFromThePreviousCalendarMonth() {
        LocalDate november = LocalDate.of(2026, 11, 1);
        LocalDate october = LocalDate.of(2026, 10, 1);
        given(balances.existsActiveBalanceOn(october)).willReturn(true);
        given(balances.copyBalances(october, november)).willReturn(7);

        BalanceCopyResponseDTO result = service.copyPreviousMonthBalances(november);

        assertThat(result).isEqualTo(new BalanceCopyResponseDTO(october, november, 7));
    }

    @Test
    void januaryCopiesFromDecemberOfThePreviousYear() {
        LocalDate january = LocalDate.of(2027, 1, 1);
        LocalDate december = LocalDate.of(2026, 12, 1);
        given(balances.existsActiveBalanceOn(december)).willReturn(true);
        given(balances.copyBalances(december, january)).willReturn(3);

        assertThat(service.copyPreviousMonthBalances(january).fromDate()).isEqualTo(december);
    }

    @Test
    void reportsZeroWhenEveryAccountAlreadyHasABalance() {
        LocalDate november = LocalDate.of(2026, 11, 1);
        given(balances.existsActiveBalanceOn(LocalDate.of(2026, 10, 1))).willReturn(true);
        given(balances.copyBalances(any(), any())).willReturn(0);

        assertThat(service.copyPreviousMonthBalances(november).copied()).isZero();
    }

    @Test
    void rejectsADateThatIsNotTheFirstOfAMonth() {
        assertThatThrownBy(() -> service.copyPreviousMonthBalances(LocalDate.of(2026, 3, 31)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("2026-03-01");
        verify(balances, never()).copyBalances(any(), any());
    }

    @Test
    void rejectsWhenThePreviousMonthHasNoActiveBalances() {
        given(balances.existsActiveBalanceOn(LocalDate.of(2026, 10, 1))).willReturn(false);

        assertThatThrownBy(() -> service.copyPreviousMonthBalances(LocalDate.of(2026, 11, 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("No active account balances for October 2026 to copy");
        verify(balances, never()).copyBalances(any(), any());
    }
}
