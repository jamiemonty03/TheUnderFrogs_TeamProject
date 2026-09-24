package com.neueda.positionservice.models;

import java.io.Serializable;
import java.util.Objects;

// A plain class, not a record: Hibernate 6.2 cannot bind a record as an @IdClass.
public class PositionId implements Serializable {

    private String accountId;
    private String symbol;

    protected PositionId() {
    }

    public PositionId(String accountId, String symbol) {
        this.accountId = accountId;
        this.symbol = symbol;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionId other)) return false;
        return Objects.equals(accountId, other.accountId) && Objects.equals(symbol, other.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, symbol);
    }
}
