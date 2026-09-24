package com.neueda.positionservice.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.persistence.*;

@IdClass(PositionId.class)
@Entity
@Table(name = "positions")
public class Position {

    @Id
    @Column(name = "account_id")
    @NotBlank(message = "Account ID cannot be null or blank")
    private String accountId;
    
    @Id
    @Column(name = "symbol")
    @NotBlank(message = "Symbol cannot be null or blank")
    private String symbol;
    
    @Column(name = "quantity")
    @NotNull(message = "Quantity cannot be null")
    @PositiveOrZero(message = "Quantity cannot be negative")
    private BigDecimal quantity;
    
    @Column(name = "average_cost")
    @NotNull(message = "Average cost cannot be null")
    @PositiveOrZero(message = "Average cost cannot be negative")
    private BigDecimal averageCost;
    @Column(name = "version")
    private int version;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "updated_by")
    private String updatedBy;

    public Position() {
        this.version = 0;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.lastUpdated = now;
    }

    @PreUpdate
    void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    public Position(String accountId, String symbol, BigDecimal quantity, BigDecimal averageCost) {
        if (accountId == null || accountId.trim().isEmpty()) {
        throw new IllegalArgumentException("Account ID cannot be null or blank");
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        if (averageCost == null) {
            throw new IllegalArgumentException("Average cost cannot be null");
        }
        
        this.accountId = accountId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageCost = averageCost;
        this.version = 0;
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
