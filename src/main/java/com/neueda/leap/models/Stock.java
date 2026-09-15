package com.neueda.leap.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an equity security (stock) in the trading system.
 * 
 * Complete stock data including fundamental metrics, valuations, and profitability:
 * - Company information (sector, industry, country, website)
 * - Market metrics (market cap, shares outstanding, beta)
 * - Valuation ratios (trailing P/E, forward P/E, price-to-book)
 * - Profitability metrics (earnings per share, return on equity, revenue)
 * - Dividend metrics (dividend rate, payout ratio)
 * 
 * Inherits common asset properties from Asset superclass.
 * 
 * @see Asset
 */
public class Stock extends Asset {

    private String sector;
    private String industry;
    private String country;
    private Integer fullTimeEmployees;
    private BigDecimal beta;
    private BigDecimal trailingPe;
    private BigDecimal forwardPe;
    private BigDecimal trailingEps;
    private BigDecimal dividendRate;
    private BigDecimal payoutRatio;
    private BigDecimal priceToBook;
    private BigDecimal returnOnEquity;
    private BigDecimal marketCap;
    private BigDecimal sharesOutstanding;
    private BigDecimal totalRevenue;
    private String website;
    
    /**
     * No-arg constructor for framework use (ORM, JSON deserialization).
     */
    public Stock() {
        super();
    }

    /**
     * Constructor for creating a stock with all available attributes.
     */
    public Stock(String symbol, String name, BigDecimal price, LocalDateTime tradeDate,
            String sector, String industry, String country, Integer fullTimeEmployees,
            BigDecimal beta, BigDecimal trailingPe, BigDecimal forwardPe, BigDecimal trailingEps,
            BigDecimal dividendRate, BigDecimal payoutRatio, BigDecimal priceToBook,
            BigDecimal returnOnEquity, BigDecimal marketCap, BigDecimal sharesOutstanding,
            BigDecimal totalRevenue, String website) {
        super(symbol, name, price, tradeDate);
        this.sector = sector;
        this.industry = industry;
        this.country = country;
        this.fullTimeEmployees = fullTimeEmployees;
        this.beta = beta;
        this.trailingPe = trailingPe;
        this.forwardPe = forwardPe;
        this.trailingEps = trailingEps;
        this.dividendRate = dividendRate;
        this.payoutRatio = payoutRatio;
        this.priceToBook = priceToBook;
        this.returnOnEquity = returnOnEquity;
        this.marketCap = marketCap;
        this.sharesOutstanding = sharesOutstanding;
        this.totalRevenue = totalRevenue;
        this.website = website;
    }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Integer getFullTimeEmployees() { return fullTimeEmployees; }
    public void setFullTimeEmployees(Integer fullTimeEmployees) { this.fullTimeEmployees = fullTimeEmployees; }

    public BigDecimal getBeta() { return beta; }
    public void setBeta(BigDecimal beta) { this.beta = beta; }

    public BigDecimal getTrailingPe() { return trailingPe; }
    public void setTrailingPe(BigDecimal trailingPe) { this.trailingPe = trailingPe; }

    public BigDecimal getForwardPe() { return forwardPe; }
    public void setForwardPe(BigDecimal forwardPe) { this.forwardPe = forwardPe; }

    public BigDecimal getTrailingEps() { return trailingEps; }
    public void setTrailingEps(BigDecimal trailingEps) { this.trailingEps = trailingEps; }

    public BigDecimal getDividendRate() { return dividendRate; }
    public void setDividendRate(BigDecimal dividendRate) { this.dividendRate = dividendRate; }

    public BigDecimal getPayoutRatio() { return payoutRatio; }
    public void setPayoutRatio(BigDecimal payoutRatio) { this.payoutRatio = payoutRatio; }

    public BigDecimal getPriceToBook() { return priceToBook; }
    public void setPriceToBook(BigDecimal priceToBook) { this.priceToBook = priceToBook; }

    public BigDecimal getReturnOnEquity() { return returnOnEquity; }
    public void setReturnOnEquity(BigDecimal returnOnEquity) { this.returnOnEquity = returnOnEquity; }

    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }

    public BigDecimal getSharesOutstanding() { return sharesOutstanding; }
    public void setSharesOutstanding(BigDecimal sharesOutstanding) { this.sharesOutstanding = sharesOutstanding; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    @Override
    public String toString() {
        return "Stock{" +
                "symbol='" + getSymbol() + '\'' +
                ", sector='" + sector + '\'' +
                ", industry='" + industry + '\'' +
                ", country='" + country + '\'' +
                ", marketCap=" + marketCap +
                ", beta=" + beta +
                ", trailingPe=" + trailingPe +
                ", website='" + website + '\'' +
                '}';
    }
}