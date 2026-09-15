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
    
    /** Unique trading symbol (e.g., "AAPL", "MSFT", "SPY") - Primary key */
    private String symbol;
    
    /** Full name of the instrument (e.g., "Apple Inc.") */
    private String name;
    
    /** Instrument classification (Equity, Bond, ETF) */
    private String instrumentType;
    
    /** Base currency for pricing (e.g., "USD", "EUR") */
    private String currency;
    
    /** Exchange where instrument trades (e.g., "NASDAQ", "NYSE") */
    private String exchange;
    
    /** Whether this instrument can be traded (false = disabled from trading) */
    private boolean tradable;
    
    /** Version counter for optimistic locking - incremented on each update */
    private int version;
    
    /** Timestamp when instrument was added to system */
    private LocalDateTime createdAt;
    
    /** Timestamp of last metadata update */
    private LocalDateTime lastUpdated;
    
    /** User or system that last updated this instrument */
    private String updatedBy;

    public Instrument() {}

    public Instrument(String symbol, String name, String instrumentType, String currency, String exchange, boolean tradable) {
        this.symbol = symbol;
        this.name = name;
        this.instrumentType = instrumentType;
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

    public String getinstrumentType() {
        return instrumentType;
    }

    public void setinstrumentType(String instrumentType) {
        this.instrumentType = instrumentType;
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
                ", instrumentType='" + instrumentType + '\'' +
                ", currency='" + currency + '\'' +
                ", exchange='" + exchange + '\'' +
                ", tradable=" + tradable +
                ", version=" + version +
                '}';
    }
}
