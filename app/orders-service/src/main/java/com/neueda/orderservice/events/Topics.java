package com.neueda.orderservice.events;

public final class Topics {

    public static final String ORDERS = "orders";
    public static final String TRADE_EVENTS = "trade-events";
    public static final String MARKET_DATA = "market-data";

    public static final String ORDERS_DLT = ORDERS + ".DLT";
    public static final String TRADE_EVENTS_DLT = TRADE_EVENTS + ".DLT";
    public static final String MARKET_DATA_DLT = MARKET_DATA + ".DLT";

    private Topics() {
    }
}
