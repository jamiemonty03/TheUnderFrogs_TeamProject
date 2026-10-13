package com.neueda.positionservice.models;

import java.io.Serializable;
import java.util.Objects;

// A plain class, not a record: Hibernate 6.2 cannot bind a record as an @IdClass.
// A holding is identified by its portfolio and symbol; the account is implied by the portfolio.
public class PositionId implements Serializable {

    private String portfolioId;
    private String symbol;

    protected PositionId() {
    }

    public PositionId(String portfolioId, String symbol) {
        this.portfolioId = portfolioId;
        this.symbol = symbol;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionId other)) return false;
        return Objects.equals(portfolioId, other.portfolioId) && Objects.equals(symbol, other.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, symbol);
    }
}
