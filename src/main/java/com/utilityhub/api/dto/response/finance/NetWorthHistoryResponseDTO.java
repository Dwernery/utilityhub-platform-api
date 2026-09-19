package com.utilityhub.api.dto.response.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class NetWorthHistoryResponseDTO {
    private LocalDate date;
    private String source; // "historical" or "account-based"
    private BigDecimal netWorth; // Total net worth
    private List<AccountBalanceDTO> accounts; // null for historical, list of accounts for account-based

    public NetWorthHistoryResponseDTO(LocalDate date, String source, BigDecimal netWorth,
            List<AccountBalanceDTO> accounts) {
        this.date = date;
        this.source = source;
        this.netWorth = netWorth;
        this.accounts = accounts;
    }

    public LocalDate getDate() {
        return this.date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public BigDecimal getNetWorth() {
        return this.netWorth;
    }

    public void setNetWorth(BigDecimal netWorth) {
        this.netWorth = netWorth;
    }

    public List<AccountBalanceDTO> getAccounts() {
        return this.accounts;
    }

    public void setAccounts(List<AccountBalanceDTO> accounts) {
        this.accounts = accounts;
    }
}
