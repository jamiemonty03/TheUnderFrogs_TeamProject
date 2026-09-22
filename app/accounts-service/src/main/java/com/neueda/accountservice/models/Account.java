package com.neueda.accountservice.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.neueda.accountservice.enums.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class Account {
   
    @NotBlank(message = "Account ID cannot be null or blank")
    private String accountId;
    
    @NotBlank(message = "Holder name cannot be null or blank")
    private String holderName;
    
    @NotNull(message = "Cash balance cannot be null")
    private BigDecimal cashBalance;
    
    @NotNull(message = "Account status cannot be null")
    private AccountStatus status;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String updatedBy;

    public Account() {}

    public Account(String accountId, String holderName, BigDecimal cashBalance, AccountStatus status) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or blank");
        }
        if (holderName == null || holderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Holder name cannot be null or blank");
        }
        if (cashBalance == null) {
            throw new IllegalArgumentException("Cash balance cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Account status cannot be null");
        }
        this.accountId = accountId;
        this.holderName = holderName;
        this.cashBalance = cashBalance;
        this.status = status;
        this.version = 0;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId='" + accountId + '\'' +
                ", holderName='" + holderName + '\'' +
                ", cashBalance=" + cashBalance +
                ", status=" + status +
                ", version=" + version +
                '}';
    }
}
