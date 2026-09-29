package com.neueda.orderservice.events;

import org.springframework.context.ApplicationEvent;

/** Internal signal registered in the placement transaction for after-commit publishing. */
public class OrderPlacedApplicationEvent extends ApplicationEvent {

    private final OrderPlacedPayload payload;

    public OrderPlacedApplicationEvent(Object source, OrderPlacedPayload payload) {
        super(source);
        this.payload = payload;
    }

    public OrderPlacedPayload getPayload() {
        return payload;
    }
}
