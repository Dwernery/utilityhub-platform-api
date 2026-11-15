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

import com.utilityhub.api.config.SecurityConfig;
import com.utilityhub.api.db.entity.finance.TransactionType;
import com.utilityhub.api.dto.response.finance.AccountResponseDTO;
import com.utilityhub.api.dto.response.finance.IncomeSourceResponseDTO;
import com.utilityhub.api.dto.response.finance.TransactionResponseDTO;
import com.utilityhub.api.service.FinanceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@WebMvcTest(FinanceController.class)
@Import(SecurityConfig.class)
class FinanceControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private FinanceService financeService;

    @Test
    void getAllAccounts_returnsAccountsFromService() {
        given(financeService.getAllAccounts())
                .willReturn(List.of(new AccountResponseDTO(1, "Checking", "Bank", "ASSET")));

        assertThat(mvc.get().uri("/api/finance/accounts"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].accountName")
                .isEqualTo("Checking");
    }

    @Test
    void createAccount_returnsCreatedAccount() {
        given(financeService.createAccount(any())).willReturn(new AccountResponseDTO(7, "Checking", "Bank", "ASSET"));

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
    void deleteIncomeSource_takesIdFromPath() {
        assertThat(mvc.delete().uri("/api/finance/income-sources/9")).hasStatus(204);

        verify(financeService).deleteIncomeSource(9);
    }
}
