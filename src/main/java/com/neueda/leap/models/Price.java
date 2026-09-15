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
 * - Creation timestamp (immutable, set once when record is created)
 * 
 * Used for:
 * - Price history lookup
 * - Technical analysis and charting
 * - Historical performance calculations
 * - ETL price data ingestion
 * 
 * Note: Price records are immutable historical data. Only createdAt is tracked.
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
    private LocalDateTime createdAt;

    /**
     * No-arg constructor for framework use (ORM, JSON deserialization).
     */
    public Price() {
    }

    /**
     * Constructor for creating price data with OHLCV values.
     * Automatically sets createdAt to current time.
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
        this.createdAt = LocalDateTime.now();
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

    /**
     * Get creation timestamp.
     * Immutable - set once when price record is created.
     * @return timestamp when this price record was created
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
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
                ", createdAt=" + createdAt +
                '}';
    }
}