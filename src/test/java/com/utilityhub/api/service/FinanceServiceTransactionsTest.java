package com.utilityhub.api.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.utilityhub.api.db.entity.finance.Transaction;
import com.utilityhub.api.db.entity.finance.TransactionType;
import com.utilityhub.api.db.repository.finance.AccountMonthlyBalanceRepository;
import com.utilityhub.api.db.repository.finance.AccountRepository;
import com.utilityhub.api.db.repository.finance.AccountTypeRepository;
import com.utilityhub.api.db.repository.finance.IncomeSourceRepository;
import com.utilityhub.api.db.repository.finance.NetWorthSnapshotRepository;
import com.utilityhub.api.db.repository.finance.TransactionRepository;
import com.utilityhub.api.dto.request.TransactionUpdateRequestDTO;
import com.utilityhub.api.dto.response.finance.TransactionResponseDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/** Plain unit test with mocked repositories: editing income and expense items. */
class FinanceServiceTransactionsTest {

    private TransactionRepository transactions;
    private FinanceService service;
    private Transaction gym;

    @BeforeEach
    void setUp() {
        transactions = mock(TransactionRepository.class);
        service = new FinanceService(
                mock(NetWorthSnapshotRepository.class),
                mock(AccountMonthlyBalanceRepository.class),
                transactions,
                mock(AccountRepository.class),
                mock(AccountTypeRepository.class),
                mock(IncomeSourceRepository.class));
        gym = new Transaction();
        gym.setId(8);
        gym.setName("Gym");
        gym.setTransactionType(TransactionType.EXPENSE);
        gym.setAmount(new BigDecimal("45.00"));
        gym.setCash(true);
        given(transactions.findById(8)).willReturn(Optional.of(gym));
        given(transactions.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void editTransaction_setsTheCashFlag() {
        TransactionResponseDTO result = service.editTransaction(
                8, new TransactionUpdateRequestDTO("Gym", new BigDecimal("45.00"), false, false));

        assertThat(result.cash()).isFalse();
        assertThat(gym.isCash()).isFalse();
    }

    @Test
    void editTransaction_withoutCashKeepsTheCurrentValue() {
        // Clients from before cash was editable leave it out; that must not clear it.
        TransactionResponseDTO result = service.editTransaction(
                8, new TransactionUpdateRequestDTO(" Gym membership ", new BigDecimal("50.00"), true, null));

        assertThat(result.cash()).isTrue();
        assertThat(result.name()).isEqualTo("Gym membership");
        assertThat(result.paid()).isTrue();
    }
}
