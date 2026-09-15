package com.neueda.leap.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents historical price data for a financial instrument (OHLCV data).
 * 
 * Tracks daily price movements and trading volume for technical analysis:
 * - Open, High, Low, Close (OHLC) prices for a trading day
 * - Trading volume in shares/units
 * - Audit trail (creation timestamp, last update, version tracking)
 * 
 * Used for:
 * - Price history lookup
 * - Technical analysis and charting
 * - Historical performance calculations
 * - ETL price data ingestion
 * 
 * @see Asset
 * @see Instrument
 */
public class Price {

    private String symbol;
    private LocalDate tradeDate;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String updatedBy;

    /**
     * No-arg constructor for framework use (ORM, JSON deserialization).
     */
    public Price() {
    }

    /**
     * Constructor for creating price data with OHLCV values.
     * Automatically sets createdAt to current time and version to 0.
     * 
     * @param symbol unique trading symbol (e.g., "AAPL")
     * @param tradeDate date of the trading day
     * @param open opening price
     * @param high highest price during the day
     * @param low lowest price during the day
     * @param close closing price
     * @param volume total shares/units traded
     */
    public Price(
            String symbol,
            LocalDate tradeDate,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume) {

        this.symbol = symbol;
        this.tradeDate = tradeDate;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
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

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Get creation timestamp.
     * Immutable - set once when price record is created.
     * @return timestamp when this price record was created
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Get last update timestamp.
     * Updated whenever price data is modified (e.g., correction, status change).
     * @return timestamp of last modification
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Set last update timestamp.
     * Should be updated whenever this price record is modified.
     * @param lastUpdated the new update timestamp
     */
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Get the user or system that last updated this price record.
     * @return identifier of last updater
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Set the user or system that last updated this price record.
     * @param updatedBy identifier of the updater
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "Price{" +
                "symbol='" + symbol + '\'' +
                ", tradeDate=" + tradeDate +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                ", version=" + version +
                ", createdAt=" + createdAt +
                ", lastUpdated=" + lastUpdated +
                ", updatedBy='" + updatedBy + '\'' +
                '}';
    }
}