package com.utilityhub.api.dto.response.finance;

import java.math.BigDecimal;

public class AccountBalanceDTO {
    private Integer accountId;
    private String accountName;
    private String accountType;
    private String category;
    private BigDecimal balance;

    public AccountBalanceDTO(Integer accountId, String accountName, String accountType, String category,
            BigDecimal balance) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountType = accountType;
        this.category = category;
        this.balance = balance;
    }

    public Integer getAccountId() {
        return this.accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return this.accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountType() {
        return this.accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
