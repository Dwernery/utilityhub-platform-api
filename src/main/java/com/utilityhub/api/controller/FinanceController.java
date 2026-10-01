package com.utilityhub.api.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.utilityhub.api.service.FinanceService;
import com.utilityhub.api.dto.request.AccountCreateRequestDTO;
import com.utilityhub.api.dto.request.DeleteAccountBalanceRequestDTO;
import com.utilityhub.api.dto.request.EditAccountBalanceRequestDTO;
import com.utilityhub.api.dto.request.EditTransactionRequestDTO;
import com.utilityhub.api.dto.response.finance.AccountResponseDTO;
import com.utilityhub.api.dto.response.finance.NetWorthHistoryResponseDTO;
import com.utilityhub.api.dto.response.finance.TransactionResponseDTO;

@RestController
@RequestMapping("/api/finance")
@Tag(name = "Finance Controller", description = "Endpoints for managing finance resources")
public class FinanceController {
    private static final Logger logger = LoggerFactory.getLogger(FinanceController.class);
    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/net-worth-history")
    public List<NetWorthHistoryResponseDTO> getNetWorthHistory() {
        return financeService.getCompleteNetWorthHistory();
    }

    @PutMapping("/account-balance")
    public ResponseEntity<String> updateAccountBalance(@RequestBody EditAccountBalanceRequestDTO request) {
        try {
            logger.info("Received PUT request to update account balance: accountId={}, balanceDate={}, balance={}",
                    request.accountId(), request.balanceDate(), request.balance());
            financeService.updateAccountBalance(request);
            logger.info("Account balance updated successfully for accountId={}", request.accountId());
            return ResponseEntity.ok("Account balance updated successfully");
        } catch (Exception e) {
            logger.error("Error updating account balance: accountId={}, balanceDate={}, error={}",
                    request.accountId(), request.balanceDate(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/transactions")
    public List<TransactionResponseDTO> getAllTransactions() {
        return financeService.getAllTransactions();
    }

    @GetMapping("/accounts")
    public List<AccountResponseDTO> getAllAccounts() {
        return financeService.getAllAccounts();
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponseDTO> createAccount(@RequestBody AccountCreateRequestDTO request) {
        try {
            logger.info("Received POST request to create account: accountName={}, accountType={}, category={}",
                    request.accountName(), request.accountType(), request.category());
            AccountResponseDTO account = financeService.createAccount(request);
            logger.info("Account created successfully: id={}, name={}", account.getId(), account.getAccountName());
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            logger.error("Error creating account: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/account-balance")
    public ResponseEntity<String> deleteAccountBalance(@RequestBody DeleteAccountBalanceRequestDTO request) {
        try {
            logger.info("Received DELETE request to delete account balance: accountId={}, balanceDate={}",
                    request.accountId(), request.balanceDate());
            financeService.deleteAccountBalance(request);
            logger.info("Account balance deleted successfully for accountId={}", request.accountId());
            return ResponseEntity.ok("Account balance deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting account balance: accountId={}, balanceDate={}, error={}",
                    request.accountId(), request.balanceDate(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/transactions")
    public ResponseEntity<String> editTransaction(@RequestBody EditTransactionRequestDTO request) {
        try {
            logger.info("Received PUT request to edit transaction: id={}, name={}, amount={}, paid={}",
                    request.id(), request.name(), request.amount(), request.paid());
            financeService.editTransaction(request);
            logger.info("Transaction edited successfully: id={}", request.id());
            return ResponseEntity.ok("Transaction edited successfully");
        } catch (Exception e) {
            logger.error("Error editing transaction: id={}, error={}",
                    request.id(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/transactions/clear-paid")
    public ResponseEntity<String> clearAllTransactionsPaid() {
        try {
            logger.info("Received PUT request to clear all transactions - setting paid to false");
            financeService.clearAllTransactionsPaid();
            logger.info("All transactions cleared successfully");
            return ResponseEntity.ok("All transactions cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing all transactions: error={}",
                    e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
