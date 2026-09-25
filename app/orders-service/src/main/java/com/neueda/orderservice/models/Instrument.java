package com.neueda.orderservice.models;

import java.math.BigDecimal;

public class Instrument {
    private String symbol;
    private String name;
    private BigDecimal price;
    private boolean tradable;

    public Instrument() {
    }

    public Instrument(String symbol, String name, BigDecimal price, boolean tradable) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.tradable = tradable;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isTradable() {
        return tradable;
    }

    public void setTradable(boolean tradable) {
        this.tradable = tradable;
    }
}
