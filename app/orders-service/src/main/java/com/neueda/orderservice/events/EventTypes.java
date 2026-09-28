package com.neueda.orderservice.events;

public final class EventTypes {

    public static final String ORDER_PLACED = "ORDER_PLACED";

    public static final String ORDER_FILLED = "ORDER_FILLED";
    public static final String ORDER_REJECTED = "ORDER_REJECTED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";

    public static final String PRICE_UPDATED = "PRICE_UPDATED";

    private EventTypes() {
    }
}
