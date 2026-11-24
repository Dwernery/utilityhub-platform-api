package com.utilityhub.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.utilityhub.api.db.entity.finance.Account;
import com.utilityhub.api.db.entity.finance.AccountCategory;
import com.utilityhub.api.db.entity.finance.AccountMonthlyBalance;
import com.utilityhub.api.db.entity.finance.AccountType;
import com.utilityhub.api.db.repository.finance.AccountMonthlyBalanceRepository;
import com.utilityhub.api.db.repository.finance.AccountRepository;
import com.utilityhub.api.db.repository.finance.AccountTypeRepository;
import com.utilityhub.api.db.repository.finance.IncomeSourceRepository;
import com.utilityhub.api.db.repository.finance.NetWorthSnapshotRepository;
import com.utilityhub.api.db.repository.finance.TransactionRepository;
import com.utilityhub.api.dto.request.AccountUpdateRequestDTO;
import com.utilityhub.api.dto.response.finance.AccountResponseDTO;
import com.utilityhub.api.dto.response.finance.NetWorthHistoryResponseDTO;
import com.utilityhub.api.exception.InvalidRequestException;
import com.utilityhub.api.exception.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Plain unit test with mocked repositories: editing and archiving accounts. */
class FinanceServiceAccountsTest {

    private AccountRepository accounts;
    private AccountTypeRepository types;
    private AccountMonthlyBalanceRepository balances;
    private NetWorthSnapshotRepository snapshots;
    private FinanceService service;

    private final AccountType cash = type(1, "Cash & Savings", AccountCategory.ASSET);
    private final AccountType investments = type(2, "Investments", AccountCategory.ASSET);
    private final AccountType mortgage = type(3, "Mortgage", AccountCategory.LIABILITY);

    @BeforeEach
    void setUp() {
        accounts = mock(AccountRepository.class);
        types = mock(AccountTypeRepository.class);
        balances = mock(AccountMonthlyBalanceRepository.class);
        snapshots = mock(NetWorthSnapshotRepository.class);
        service = new FinanceService(
                snapshots,
                balances,
                mock(TransactionRepository.class),
                accounts,
                types,
                mock(IncomeSourceRepository.class));
        given(accounts.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(types.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    private static AccountType type(int id, String name, AccountCategory category) {
        AccountType type = new AccountType();
        type.setId(id);
        type.setName(name);
        type.setCategory(category);
        return type;
    }

    private static Account account(int id, String name, AccountType type) {
        Account account = new Account();
        account.setId(id);
        account.setName(name);
        account.setAccountType(type);
        return account;
    }

    @Test
    void renamesAndRetypesWithinTheCategory() {
        given(accounts.findById(5)).willReturn(Optional.of(account(5, "Chekcing", cash)));
        given(types.findByName("Investments")).willReturn(Optional.of(investments));

        AccountResponseDTO result =
                service.updateAccount(5, new AccountUpdateRequestDTO("  Brokerage ", "Investments", false));

        assertThat(result).isEqualTo(new AccountResponseDTO(5, "Brokerage", "Investments", "ASSET", false));
    }

    @Test
    void aNewTypeNameIsCreatedInTheAccountsOwnCategory() {
        given(accounts.findById(7)).willReturn(Optional.of(account(7, "Home loan", mortgage)));
        given(types.findByName("HELOC")).willReturn(Optional.empty());

        AccountResponseDTO result = service.updateAccount(7, new AccountUpdateRequestDTO("Home loan", "HELOC", false));

        assertThat(result.accountType()).isEqualTo("HELOC");
        assertThat(result.category()).isEqualTo("LIABILITY");
        verify(types).save(org.mockito.ArgumentMatchers.argThat(t -> t.getCategory() == AccountCategory.LIABILITY));
    }

    @Test
    void refusesATypeFromTheOtherCategory() {
        // Turning an asset into a liability would rewrite every past month's net worth.
        given(accounts.findById(5)).willReturn(Optional.of(account(5, "Checking", cash)));
        given(types.findByName("Mortgage")).willReturn(Optional.of(mortgage));

        assertThatThrownBy(() -> service.updateAccount(5, new AccountUpdateRequestDTO("Checking", "Mortgage", false)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("already exists as LIABILITY");
        verify(accounts, never()).save(any());
    }

    @Test
    void archivesAndUnarchives() {
        Account checking = account(5, "Checking", cash);
        given(accounts.findById(5)).willReturn(Optional.of(checking));
        given(types.findByName("Cash & Savings")).willReturn(Optional.of(cash));

        assertThat(service.updateAccount(5, new AccountUpdateRequestDTO("Checking", "Cash & Savings", true))
                        .archived())
                .isTrue();
        assertThat(service.updateAccount(5, new AccountUpdateRequestDTO("Checking", "Cash & Savings", false))
                        .archived())
                .isFalse();
    }

    @Test
    void anUnknownAccountIsNotFound() {
        given(accounts.findById(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAccount(99, new AccountUpdateRequestDTO("X", "Cash & Savings", false)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void archivedAccountsStayInTheNetWorthHistory() {
        Account closed = account(5, "Old Savings", cash);
        closed.setArchived(true);
        Account open = account(6, "Checking", cash);
        AccountMonthlyBalance closedBalance = new AccountMonthlyBalance();
        closedBalance.setAccount(closed);
        closedBalance.setBalanceDate(LocalDate.of(2026, 9, 1));
        closedBalance.setBalance(new BigDecimal("1000.00"));
        AccountMonthlyBalance openBalance = new AccountMonthlyBalance();
        openBalance.setAccount(open);
        openBalance.setBalanceDate(LocalDate.of(2026, 9, 1));
        openBalance.setBalance(new BigDecimal("250.00"));
        given(snapshots.findAllByOrderByBalanceMonthAsc()).willReturn(List.of());
        given(balances.findAllAccountBalancesWithDetails()).willReturn(List.of(closedBalance, openBalance));

        List<NetWorthHistoryResponseDTO> history = service.getCompleteNetWorthHistory();

        assertThat(history).hasSize(1);
        assertThat(history.get(0).netWorth()).isEqualByComparingTo("1250.00");
        assertThat(history.get(0).accounts())
                .extracting("accountName")
                .containsExactlyInAnyOrder("Old Savings", "Checking");
    }
}
