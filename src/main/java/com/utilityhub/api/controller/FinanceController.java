package com.utilityhub.api.controller;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import com.utilityhub.api.service.FinanceService;
import com.utilityhub.api.dto.response.finance.NetWorthHistoryResponseDTO;

@RestController
@RequestMapping("/api/finance")
@Tag(name = "Finance Controller", description = "Endpoints for managing finance resources")
public class FinanceController {
    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/net-worth-history")
    public List<NetWorthHistoryResponseDTO> getNetWorthHistory() {
        return financeService.getCompleteNetWorthHistory();
    }

}
