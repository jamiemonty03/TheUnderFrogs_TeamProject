package com.neueda.orderservice.models;

import java.math.BigDecimal;
import com.neueda.orderservice.enums.AccountStatus;

public class Account {
    private String accountId;
    private String holderName;
    private BigDecimal cashBalance;
    private AccountStatus status;

    public Account() {
    }

    public Account(String accountId, String holderName, BigDecimal cashBalance, AccountStatus status) {
        this.accountId = accountId;
        this.holderName = holderName;
        this.cashBalance = cashBalance;
        this.status = status;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }
}
