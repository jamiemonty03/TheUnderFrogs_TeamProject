package com.neueda.leap.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Abstract superclass representing a financial asset (stock, bond, ETF).
 * 
 * Defines common properties shared by all asset types:
 * - Unique symbol identifier
 * - Current market price
 * - Trading metadata (date, timestamps)
 * - Audit trail (version, created/updated timestamps)
 * 
 * Subclasses:
 * @see Stock
 * @see Bond
 * @see Etf
 */
public abstract class Asset {
    private String symbol;
    private String name;
    private BigDecimal price;
    private LocalDateTime tradeDate;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String updatedBy;


    public Asset() {}

    public Asset(String symbol, String name, BigDecimal price, LocalDateTime tradeDate) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.tradeDate = tradeDate;
        this.version = 0;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDateTime tradeDate) {
        this.tradeDate = tradeDate;
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
        return getClass().getSimpleName() + "{" +
                "symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", tradeDate=" + tradeDate +
                ", version=" + version +
                '}';
    }
}