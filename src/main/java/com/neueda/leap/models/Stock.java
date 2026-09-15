package com.neueda.leap.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an equity security (stock) in the trading system.
 * 
 * A Stock is a type of asset representing ownership in a public company.
 * It inherits common asset properties (symbol, price, trade date)
 * and can be used to track individual stock holdings and trades.
 * 
 * @see Asset
 */
public class Stock extends Asset {

    private BigDecimal marketCap;
    private BigDecimal sharesOutstanding;
    private BigDecimal peRatio;
    private BigDecimal earningPerShare;
    private BigDecimal dividendYield;
    
    /**
     * No-arg constructor for framework use (ORM, JSON deserialization).
     */
    public Stock() {
        super();
    }

    /**
     * Constructor for creating a stock with asset properties and stock-specific details.
     * @param symbol unique trading symbol (e.g., "AAPL", "MSFT")
     * @param name company name
     * @param price current share price
     * @param tradeDate date of trading
     * @param marketCap total market capitalization
     * @param sharesOutstanding number of shares issued
     * @param peRatio price-to-earnings ratio
     * @param earningPerShare earnings per share
     * @param dividendYield annual dividend as percentage
     */
    public Stock(String symbol, String name, BigDecimal price, LocalDateTime tradeDate,
            BigDecimal marketCap, BigDecimal sharesOutstanding, BigDecimal peRatio,
            BigDecimal earningPerShare, BigDecimal dividendYield) {
        super(symbol, name, price, tradeDate);
        this.marketCap = marketCap;
        this.sharesOutstanding = sharesOutstanding;
        this.peRatio = peRatio;
        this.earningPerShare = earningPerShare;
        this.dividendYield = dividendYield;
    }

    public BigDecimal getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(BigDecimal marketCap) {
        this.marketCap = marketCap;
    }

    public BigDecimal getSharesOutstanding() {
        return sharesOutstanding;
    }

    public void setSharesOutstanding(BigDecimal sharesOutstanding) {
        this.sharesOutstanding = sharesOutstanding;
    }

    public BigDecimal getPeRatio() {
        return peRatio;
    }

    public void setPeRatio(BigDecimal peRatio) {
        this.peRatio = peRatio;
    }

    public BigDecimal getEarningPerShare() {
        return earningPerShare;
    }

    public void setEarningPerShare(BigDecimal earningPerShare) {
        this.earningPerShare = earningPerShare;
    }

    public BigDecimal getDividendYield() {
        return dividendYield;
    }

    public void setDividendYield(BigDecimal dividendYield) {
        this.dividendYield = dividendYield;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "symbol='" + getSymbol() + '\'' +
                ", name='" + getName() + '\'' +
                ", price=" + getPrice() +
                ", marketCap=" + marketCap +
                ", sharesOutstanding=" + sharesOutstanding +
                ", peRatio=" + peRatio +
                ", earningPerShare=" + earningPerShare +
                ", dividendYield=" + dividendYield +
                ", version=" + getVersion() +
                '}';