package com.neueda.leap.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an Exchange-Traded Fund (ETF) in the trading system.
 * 
 * An ETF is a type of asset representing a basket of securities (stocks, bonds, etc.)
 * that trades like a single stock on an exchange. Key characteristics:
 * - NAV (Net Asset Value): value of assets per fund share
 * - Expense ratio: annual management fee as percentage of assets
 * - Assets under management: total value managed by the fund
 * - Tracking error: deviation from index benchmark
 * 
 * Inherits common asset properties from Asset superclass.
 * 
 * @see Asset
 */
public class Etf extends Asset {
    
    private BigDecimal nav;
    private BigDecimal expenseRatio;
    private BigDecimal assetsUnderManagement;
    private BigDecimal trackingError;

    /**
     * No-arg constructor for framework use (ORM, JSON deserialization).
     */
    public Etf() {
        super();
    }

    /**
     * Constructor for creating an ETF with asset properties and ETF-specific details.
     * @param symbol unique trading symbol (e.g., "SPY", "QQQ", "VTI")
     * @param name fund name
     * @param price current share price
     * @param tradeDate date of trading
     * @param nav net asset value per share
     * @param expenseRatio annual management fee as percentage
     * @param assetsUnderManagement total fund assets
     * @param trackingError deviation from benchmark index
     */
    public Etf(String symbol, String name, BigDecimal price, LocalDateTime tradeDate,
            BigDecimal nav, BigDecimal expenseRatio, BigDecimal assetsUnderManagement,
            BigDecimal trackingError) {
        super(symbol, name, price, tradeDate);
        this.nav = nav;
        this.expenseRatio = expenseRatio;
        this.assetsUnderManagement = assetsUnderManagement;
        this.trackingError = trackingError;
    }

    public BigDecimal getNav() {
        return nav;
    }

    public void setNav(BigDecimal nav) {
        this.nav = nav;
    }

    public BigDecimal getExpenseRatio() {
        return expenseRatio;
    }

    public void setExpenseRatio(BigDecimal expenseRatio) {
        this.expenseRatio = expenseRatio;
    }

    public BigDecimal getAssetsUnderManagement() {
        return assetsUnderManagement;
    }

    public void setAssetsUnderManagement(BigDecimal assetsUnderManagement) {
        this.assetsUnderManagement = assetsUnderManagement;
    }

    public BigDecimal getTrackingError() {
        return trackingError;
    }

    public void setTrackingError(BigDecimal trackingError) {
        this.trackingError = trackingError;
    }

    @Override
    public String toString() {
        return "Etf{" +
                "symbol='" + getSymbol() + '\'' +
                ", name='" + getName() + '\'' +
                ", price=" + getPrice() +
                ", nav=" + nav +
                ", expenseRatio=" + expenseRatio +
                ", assetsUnderManagement=" + assetsUnderManagement +
                ", trackingError=" + trackingError +
                ", version=" + getVersion() +
                '}';
    }
}