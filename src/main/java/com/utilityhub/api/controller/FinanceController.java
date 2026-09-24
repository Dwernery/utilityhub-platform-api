package com.utilityhub.api.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.utilityhub.api.service.FinanceService;
import com.utilityhub.api.dto.request.EditAccountBalanceRequestDTO;
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

}
