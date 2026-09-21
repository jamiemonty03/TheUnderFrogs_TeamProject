package com.neueda.instrumentservice.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an Exchange-Traded Fund (ETF) in the trading system.
 * 
 * Complete ETF data including fund characteristics, performance, and valuation:
 * - Fund information (category, family, legal structure)
 * - Pricing metrics (NAV price, total/net assets)
 * - Performance metrics (YTD return, 3-year, 5-year average returns)
 * - Risk metrics (beta, tracking error, distribution yield)
 * - Expense metrics (net expense ratio)
 * 
 * Inherits common asset properties from Asset superclass.
 * 
 * @see Asset
 */
public class Etf extends Asset {
    
    private String category;
    private String fundFamily;
    private String legalType;
    private BigDecimal netExpenseRatio;
    private BigDecimal navPrice;
    private BigDecimal totalAssets;
    private BigDecimal netAssets;
    private BigDecimal ytdReturn;
    private BigDecimal threeYearAvgReturn;
    private BigDecimal fiveYearAvgReturn;
    private BigDecimal beta3Year;
    private BigDecimal distributionYield;

    public Etf() {
        super();
    }

    public Etf(String symbol, String name, BigDecimal price, LocalDateTime tradeDate,
            String category, String fundFamily, String legalType, BigDecimal netExpenseRatio,
            BigDecimal navPrice, BigDecimal totalAssets, BigDecimal netAssets,
            BigDecimal ytdReturn, BigDecimal threeYearAvgReturn, BigDecimal fiveYearAvgReturn,
            BigDecimal beta3Year, BigDecimal distributionYield) {
        super(symbol, name, price, tradeDate);
        this.category = category;
        this.fundFamily = fundFamily;
        this.legalType = legalType;
        this.netExpenseRatio = netExpenseRatio;
        this.navPrice = navPrice;
        this.totalAssets = totalAssets;
        this.netAssets = netAssets;
        this.ytdReturn = ytdReturn;
        this.threeYearAvgReturn = threeYearAvgReturn;
        this.fiveYearAvgReturn = fiveYearAvgReturn;
        this.beta3Year = beta3Year;
        this.distributionYield = distributionYield;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getFundFamily() { return fundFamily; }
    public void setFundFamily(String fundFamily) { this.fundFamily = fundFamily; }

    public String getLegalType() { return legalType; }
    public void setLegalType(String legalType) { this.legalType = legalType; }

    public BigDecimal getNetExpenseRatio() { return netExpenseRatio; }
    public void setNetExpenseRatio(BigDecimal netExpenseRatio) { this.netExpenseRatio = netExpenseRatio; }

    public BigDecimal getNavPrice() { return navPrice; }
    public void setNavPrice(BigDecimal navPrice) { this.navPrice = navPrice; }

    public BigDecimal getTotalAssets() { return totalAssets; }
    public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }

    public BigDecimal getNetAssets() { return netAssets; }
    public void setNetAssets(BigDecimal netAssets) { this.netAssets = netAssets; }

    public BigDecimal getYtdReturn() { return ytdReturn; }
    public void setYtdReturn(BigDecimal ytdReturn) { this.ytdReturn = ytdReturn; }

    public BigDecimal getThreeYearAvgReturn() { return threeYearAvgReturn; }
    public void setThreeYearAvgReturn(BigDecimal threeYearAvgReturn) { this.threeYearAvgReturn = threeYearAvgReturn; }

    public BigDecimal getFiveYearAvgReturn() { return fiveYearAvgReturn; }
    public void setFiveYearAvgReturn(BigDecimal fiveYearAvgReturn) { this.fiveYearAvgReturn = fiveYearAvgReturn; }

    public BigDecimal getBeta3Year() { return beta3Year; }
    public void setBeta3Year(BigDecimal beta3Year) { this.beta3Year = beta3Year; }

    public BigDecimal getDistributionYield() { return distributionYield; }
    public void setDistributionYield(BigDecimal distributionYield) { this.distributionYield = distributionYield; }

    @Override
    public String toString() {
        return "Etf{" +
                "symbol='" + getSymbol() + '\'' +
                ", category='" + category + '\'' +
                ", fundFamily='" + fundFamily + '\'' +
                ", navPrice=" + navPrice +
                ", totalAssets=" + totalAssets +
                ", ytdReturn=" + ytdReturn +
                ", netExpenseRatio=" + netExpenseRatio +
                '}';
    }
}
