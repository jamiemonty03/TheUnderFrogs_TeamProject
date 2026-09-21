package com.neueda.leap.models;

import java.time.LocalDateTime;

/**
 * Represents a tradable financial instrument (stock, bond, ETF, etc.).
 * 
 * Master data for all tradable securities in the system:
 * - Identifies instruments by unique symbol (e.g., "AAPL")
 * - Defines instrument characteristics (type, currency, exchange)
 * - Controls trading eligibility via tradable flag
 * 
 * Referenced by:
 * - Orders: validation that symbol exists and is tradable
 * - Positions: identifies what is being held
 * 
 * @see Order
 * @see Position
 */
public class Instrument {
    
    private String symbol;
    
    private String name;
    
    private String assetClass;
    
    private String currency;
    
    private String exchange;
    
    private boolean tradable;
    
    private int version;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime lastUpdated;
    
    private String updatedBy;

    public Instrument() {}

    public Instrument(String symbol, String name, String assetClass, String currency, String exchange, boolean tradable) {
        if (symbol == null || symbol.trim().isEmpty()) {
        throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (assetClass == null || assetClass.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset class cannot be null or blank");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or blank");
        }
        if (exchange == null || exchange.trim().isEmpty()) {
            throw new IllegalArgumentException("Exchange cannot be null or blank");
        }
        
        this.symbol = symbol;
        this.name = name;
        this.assetClass = assetClass;
        this.currency = currency;
        this.exchange = exchange;
        this.tradable = tradable;
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

    public String getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(String assetClass) {
        this.assetClass = assetClass;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public boolean isTradable() {
        return tradable;
    }

    public void setTradable(boolean tradable) {
        this.tradable = tradable;
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
        return "Instrument{" +
                "symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", assetClass='" + assetClass + '\'' +
                ", currency='" + currency + '\'' +
                ", exchange='" + exchange + '\'' +
                ", tradable=" + tradable +
                ", version=" + version +
                '}';
    }
}
