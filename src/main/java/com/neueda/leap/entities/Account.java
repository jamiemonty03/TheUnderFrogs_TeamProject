package com.neueda.leap.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    /**
     * Account status enumeration.
     * ACTIVE: Account can execute trades
     * INACTIVE: Account cannot trade but can still be viewed
     * SUSPENDED: Account is frozen, no trades or withdrawals allowed
     */
    public enum Status { ACTIVE, INACTIVE, SUSPENDED }
    
   
    private String accountId;
    private String holderName;
    private BigDecimal cashBalance;
    private Status status;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String updatedBy;

    public Account() {}

    public Account(String accountId, String holderName, BigDecimal cashBalance, Status status) {
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
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
