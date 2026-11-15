package com.utilityhub.api.controller;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utilityhub.api.dto.request.AccountBalanceUpdateRequestDTO;
import com.utilityhub.api.dto.request.AccountCreateRequestDTO;
import com.utilityhub.api.dto.request.IncomeSourceCreateRequestDTO;
import com.utilityhub.api.dto.request.IncomeSourceUpdateRequestDTO;
import com.utilityhub.api.dto.request.TransactionUpdateRequestDTO;
import com.utilityhub.api.dto.response.finance.AccountResponseDTO;
import com.utilityhub.api.dto.response.finance.IncomeSourceResponseDTO;
import com.utilityhub.api.dto.response.finance.NetWorthHistoryResponseDTO;
import com.utilityhub.api.dto.response.finance.TransactionResponseDTO;
import com.utilityhub.api.service.FinanceService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/finance")
@Tag(name = "Finance", description = "Accounts, balances, transactions and income")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/net-worth-history")
    public List<NetWorthHistoryResponseDTO> getNetWorthHistory() {
        return financeService.getCompleteNetWorthHistory();
    }

    // --- Accounts and their monthly balances ---

    @GetMapping("/accounts")
    public List<AccountResponseDTO> getAllAccounts() {
        return financeService.getAllAccounts();
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponseDTO createAccount(@Valid @RequestBody AccountCreateRequestDTO request) {
        return financeService.createAccount(request);
    }

    /** Creates the balance if it doesn't exist, replaces it if it does. */
    @PutMapping("/accounts/{accountId}/balances/{balanceDate}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putAccountBalance(
            @PathVariable Integer accountId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate balanceDate,
            @Valid @RequestBody AccountBalanceUpdateRequestDTO request) {
        financeService.updateAccountBalance(accountId, balanceDate, request.balance());
    }

    @DeleteMapping("/accounts/{accountId}/balances/{balanceDate}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccountBalance(
            @PathVariable Integer accountId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate balanceDate) {
        financeService.deleteAccountBalance(accountId, balanceDate);
    }

    // --- Transactions ---

    @GetMapping("/transactions")
    public List<TransactionResponseDTO> getAllTransactions() {
        return financeService.getAllTransactions();
    }

    @PutMapping("/transactions/{id}")
    public TransactionResponseDTO updateTransaction(
            @PathVariable Integer id, @Valid @RequestBody TransactionUpdateRequestDTO request) {
        return financeService.editTransaction(id, request);
    }

    /** Bulk action: marks every transaction as unpaid (e.g. at the start of a month). */
    @PutMapping("/transactions/clear-paid")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAllTransactionsPaid() {
        financeService.clearAllTransactionsPaid();
    }

    // --- Income sources ---

    @GetMapping("/income-sources")
    public List<IncomeSourceResponseDTO> getAllIncomeSources() {
        return financeService.getAllIncomeSources();
    }

    @PostMapping("/income-sources")
    @ResponseStatus(HttpStatus.CREATED)
    public IncomeSourceResponseDTO createIncomeSource(@Valid @RequestBody IncomeSourceCreateRequestDTO request) {
        return financeService.createIncomeSource(request);
    }

    @PutMapping("/income-sources/{id}")
    public IncomeSourceResponseDTO updateIncomeSource(
            @PathVariable Integer id, @Valid @RequestBody IncomeSourceUpdateRequestDTO request) {
        return financeService.updateIncomeSource(id, request);
    }

    @DeleteMapping("/income-sources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIncomeSource(@PathVariable Integer id) {
        financeService.deleteIncomeSource(id);
    }
}
