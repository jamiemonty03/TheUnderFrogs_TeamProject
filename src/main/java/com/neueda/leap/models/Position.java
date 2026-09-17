package com.neueda.leap.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a holding of shares/units in a specific instrument.
 * 
 * Tracks:
 * - How many units are held (quantity)
 * - The average acquisition cost per unit (FIFO basis)
 * - Composite key: (accountId, symbol) uniquely identifies a position
 * 
 * Used for:
 * - Position validation before SELL orders (sufficient holdings?)
 * - Cost basis calculation for P&L reporting
 * - FIFO average cost recalculation on BUY orders
 * 
 * @see Account
 * @see Order
 * @see Instrument
 */
public class Position {
    private String accountId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal averageCost;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String updatedBy;

    public Position() {}

    public Position(String accountId, String symbol, BigDecimal quantity, BigDecimal averageCost) {
        this.accountId = accountId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageCost = averageCost;
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

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAverageCost() {
        return averageCost;
    }

    public void setAverageCost(BigDecimal averageCost) {
        this.averageCost = averageCost;
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

    public BigDecimal getTotalCostBasis() {
        return quantity.multiply(averageCost);
    }

    public BigDecimal getMarketValue(BigDecimal currentPrice) {
        return currentPrice.multiply(quantity);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position position = (Position) obj;
        return  Objects.equals(accountId, position.accountId) &&
                Objects.equals(symbol, position.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, symbol);
    }

    @Override
    public String toString() {
        return "Position{" +
                "accountId='" + accountId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", quantity=" + quantity +
                ", averageCost=" + averageCost +
                ", version=" + version +
                '}';
    }
}
