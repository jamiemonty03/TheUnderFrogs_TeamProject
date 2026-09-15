package com.neueda.leap.models;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Represents a fixed-income security (bond) in the trading system.
 * 
 * A Bond is a type of asset representing debt obligations with specific characteristics:
 * - Yield to maturity: expected return if bond is held to maturity
 * - Coupon rate: periodic interest payment rate
 * - Duration: price sensitivity to interest rate changes
 * 
 * Inherits common asset properties from Asset superclass.
 * 
 * @see Asset
 */
public class Bond extends Asset {
    
    private BigDecimal yieldToMaturity;
    private BigDecimal couponRate;
    private BigDecimal duration;

    /**
     * No-arg constructor for framework use (ORM, JSON deserialization).
     */
    public Bond() {
        super();
    }

    /**
     * Constructor for creating a bond with asset properties and bond-specific details.
     * @param symbol unique trading symbol (e.g., "US10Y", "CORPORATE-BOND-1")
     * @param name bond description
     * @param price current bond price
     * @param tradeDate date of trading
     * @param yieldToMaturity expected return if held to maturity
     * @param couponRate periodic interest payment rate
     * @param duration price sensitivity measure
     */
    public Bond(String symbol, String name, BigDecimal price, LocalDateTime tradeDate,
            BigDecimal yieldToMaturity, BigDecimal couponRate, BigDecimal duration) {
        super(symbol, name, price, tradeDate);
        this.yieldToMaturity = yieldToMaturity;
        this.couponRate = couponRate;
        this.duration = duration;
    }

    public BigDecimal getYieldToMaturity() {
        return yieldToMaturity;
    }

    public void setYieldToMaturity(BigDecimal yieldToMaturity) {
        this.yieldToMaturity = yieldToMaturity;
    }

    public BigDecimal getCouponRate() {
        return couponRate;
    }

    public void setCouponRate(BigDecimal couponRate) {
        this.couponRate = couponRate;
    }

    public BigDecimal getDuration() {
        return duration;
    }

    public void setDuration(BigDecimal duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "Bond{" +
                "symbol='" + getSymbol() + '\'' +
                ", name='" + getName() + '\'' +
                ", price=" + getPrice() +
                ", yieldToMaturity=" + yieldToMaturity +
                ", couponRate=" + couponRate +
                ", duration=" + duration +
                ", version=" + getVersion() +
                '}';
    }
}