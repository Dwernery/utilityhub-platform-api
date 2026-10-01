package com.utilityhub.api.dto.response.finance;

public class AccountResponseDTO {
    private Integer id;
    private String accountName;
    private String accountType;
    private String category;

    public AccountResponseDTO(Integer id, String accountName, String accountType, String category) {
        this.id = id;
        this.accountName = accountName;
        this.accountType = accountType;
        this.category = category;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
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
}
