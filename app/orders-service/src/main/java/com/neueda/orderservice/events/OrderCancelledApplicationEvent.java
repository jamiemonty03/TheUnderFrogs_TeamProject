package com.neueda.orderservice.events;

import org.springframework.context.ApplicationEvent;

public class OrderCancelledApplicationEvent extends ApplicationEvent {

    private final OrderCancelledPayload payload;

    public OrderCancelledApplicationEvent(Object source, OrderCancelledPayload payload) {
        super(source);
        this.payload = payload;
    }

    public OrderCancelledPayload getPayload() {
        return payload;
    }
}
