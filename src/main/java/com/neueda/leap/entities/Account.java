package com.neueda.leap.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.exceptions.AccountNotActiveException;
import com.neueda.leap.exceptions.InsufficientFundsException;

/**
 * Represents a trading account with cash balance and position holdings.
 * 
 * Manages the financial state of an account, including:
 * - Available cash balance for trading
 * - Account status (active, inactive, suspended)
 * - Version tracking for optimistic locking in concurrent updates
 * 
 * @see Order
 * @see Position
 */
public class Account {
   
    private String accountId;
    private String holderName;
    private BigDecimal cashBalance;
    private AccountStatus status;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String updatedBy;

    public Account() {}

    public Account(String accountId, String holderName, BigDecimal cashBalance, AccountStatus status) {
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

    public void credit(BigDecimal amount) throws AccountNotActiveException {
        if (!isActive()) {
            throw new AccountNotActiveException("Cannot credit an inactive account");
        }
        this.cashBalance = this.cashBalance.add(amount);
        this.lastUpdated = LocalDateTime.now();
        this.version++;
    }

    public void debit(BigDecimal amount) throws AccountNotActiveException, InsufficientFundsException {
        if (!isActive()) {
            throw new AccountNotActiveException("Cannot debit an inactive account");
        }
        if (this.cashBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds. Balance: " + this.cashBalance + ", Requested: " + amount);
        }
        this.cashBalance = this.cashBalance.subtract(amount);
        this.lastUpdated = LocalDateTime.now();
        this.version++;
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
