package com.utilityhub.api.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.utilityhub.api.db.entity.finance.TransactionType;
import com.utilityhub.api.dto.response.finance.AccountResponseDTO;
import com.utilityhub.api.dto.response.finance.BalanceCopyResponseDTO;
import com.utilityhub.api.dto.response.finance.IncomeSourceResponseDTO;
import com.utilityhub.api.dto.response.finance.TransactionResponseDTO;
import com.utilityhub.api.exception.InvalidRequestException;
import com.utilityhub.api.service.FinanceService;
import com.utilityhub.api.support.WithSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@WebMvcTest(FinanceController.class)
@Import(WithSession.class)
class FinanceControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private FinanceService financeService;

    @Test
    void getAllAccounts_returnsAccountsFromService() {
        given(financeService.getAllAccounts())
                .willReturn(List.of(new AccountResponseDTO(1, "Checking", "Bank", "ASSET", false)));

        assertThat(mvc.get().uri("/api/finance/accounts"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].accountName")
                .isEqualTo("Checking");
    }

    @Test
    void createAccount_returnsCreatedAccount() {
        given(financeService.createAccount(any()))
                .willReturn(new AccountResponseDTO(7, "Checking", "Bank", "ASSET", false));

        assertThat(mvc.post()
                        .uri("/api/finance/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"accountName":"Checking","accountType":"Bank","category":"ASSET"}
                        """))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(7);
    }

    @Test
    void createIncomeSource_returnsCreatedIncomeSource() {
        given(financeService.createIncomeSource(any()))
                .willReturn(new IncomeSourceResponseDTO(3, 2026, "Salary", new BigDecimal("1000.00")));

        assertThat(mvc.post()
                        .uri("/api/finance/income-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"year":2026,"name":"Salary","amount":1000.00}
                        """))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Salary");
    }

    @Test
    void createTransaction_returnsCreatedTransaction() {
        given(financeService.createTransaction(any()))
                .willReturn(new TransactionResponseDTO(
                        8, "Gym", TransactionType.EXPENSE, new BigDecimal("45.00"), false, false));

        assertThat(mvc.post()
                        .uri("/api/finance/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"name":"Gym","transactionType":"EXPENSE","amount":45.00}
                        """))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(8);

        verify(financeService).createTransaction(argThat(r -> r.transactionType() == TransactionType.EXPENSE));
    }

    @Test
    void deleteTransaction_takesIdFromPath() {
        assertThat(mvc.delete().uri("/api/finance/transactions/8")).hasStatus(204);

        verify(financeService).deleteTransaction(8);
    }

    @Test
    void updateTransaction_takesIdFromPathAndReturnsUpdatedTransaction() {
        given(financeService.editTransaction(eq(5), any()))
                .willReturn(new TransactionResponseDTO(
                        5, "Rent", TransactionType.EXPENSE, new BigDecimal("1500"), true, false));

        assertThat(mvc.put()
                        .uri("/api/finance/transactions/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"name":"Rent","amount":1500,"paid":true}
                        """))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.paid")
                .isEqualTo(true);

        verify(financeService).editTransaction(eq(5), argThat(r -> r.paid()));
    }

    @Test
    void updateTransaction_passesCashThroughAndLeavesItNullWhenMissing() {
        given(financeService.editTransaction(eq(5), any()))
                .willReturn(new TransactionResponseDTO(
                        5, "Rent", TransactionType.EXPENSE, new BigDecimal("1500"), false, true));

        assertThat(mvc.put()
                        .uri("/api/finance/transactions/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"name":"Rent","amount":1500,"paid":false,"cash":true}
                        """))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.cash")
                .isEqualTo(true);
        verify(financeService).editTransaction(eq(5), argThat(r -> Boolean.TRUE.equals(r.cash())));

        assertThat(mvc.put()
                        .uri("/api/finance/transactions/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"name":"Rent","amount":1500,"paid":false}
                        """))
                .hasStatusOk();
        verify(financeService).editTransaction(eq(6), argThat(r -> r.cash() == null));
    }

    @Test
    void putAccountBalance_parsesDateFromPathAndReturnsNoContent() {
        assertThat(mvc.put()
                        .uri("/api/finance/accounts/3/balances/2026-11-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":250.50}"))
                .hasStatus(204);

        verify(financeService).updateAccountBalance(3, LocalDate.of(2026, 11, 1), new BigDecimal("250.50"));
    }

    @Test
    void deleteAccountBalance_returnsNoContent() {
        assertThat(mvc.delete().uri("/api/finance/accounts/3/balances/2026-11-01"))
                .hasStatus(204);

        verify(financeService).deleteAccountBalance(3, LocalDate.of(2026, 11, 1));
    }

    @Test
    void updateAccount_returnsTheUpdatedAccount() {
        given(financeService.updateAccount(eq(5), any()))
                .willReturn(new AccountResponseDTO(5, "Brokerage", "Investments", "ASSET", true));

        assertThat(mvc.put()
                        .uri("/api/finance/accounts/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"accountName":"Brokerage","accountType":"Investments","archived":true}
                        """))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.archived")
                .isEqualTo(true);

        verify(financeService)
                .updateAccount(
                        eq(5), argThat(r -> r.archived() && r.accountName().equals("Brokerage")));
    }

    @Test
    void updateAccount_requiresArchived() {
        // A missing flag must not silently unarchive the account.
        assertThat(mvc.put()
                        .uri("/api/finance/accounts/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"accountName":"Brokerage","accountType":"Investments"}
                        """))
                .hasStatus(400);
    }

    @Test
    void updateAccount_aTypeFromTheOtherCategoryIsABadRequest() {
        given(financeService.updateAccount(eq(5), any()))
                .willThrow(new InvalidRequestException("Account type 'Mortgage' already exists as LIABILITY"));

        assertThat(mvc.put()
                        .uri("/api/finance/accounts/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"accountName":"Checking","accountType":"Mortgage","archived":false}
                        """))
                .hasStatus(400);
    }

    @Test
    void copyPreviousMonthBalances_returnsWhatWasCopied() {
        given(financeService.copyPreviousMonthBalances(LocalDate.of(2026, 11, 1)))
                .willReturn(new BalanceCopyResponseDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1), 7));

        var result = assertThat(mvc.post().uri("/api/finance/balances/2026-11-01/copy-previous-month"))
                .hasStatusOk()
                .bodyJson();
        result.extractingPath("$.copied").isEqualTo(7);
        result.extractingPath("$.fromDate").isEqualTo("2026-10-01");
    }

    @Test
    void copyPreviousMonthBalances_nothingToCopyIsABadRequestWithTheReason() {
        given(financeService.copyPreviousMonthBalances(any()))
                .willThrow(new InvalidRequestException("No active account balances for October 2026 to copy"));

        assertThat(mvc.post().uri("/api/finance/balances/2026-11-01/copy-previous-month"))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("No active account balances for October 2026 to copy");
    }

    @Test
    void copyPreviousMonthBalances_rejectsAMalformedDate() {
        assertThat(mvc.post().uri("/api/finance/balances/november/copy-previous-month"))
                .hasStatus(400);
    }

    @Test
    void deleteIncomeSource_takesIdFromPath() {
        assertThat(mvc.delete().uri("/api/finance/income-sources/9")).hasStatus(204);

        verify(financeService).deleteIncomeSource(9);
    }
}
