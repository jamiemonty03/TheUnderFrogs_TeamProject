package com.neueda.orderservice.events;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderMetricsListener {

    private final Counter ordersCreated;

    public OrderMetricsListener(MeterRegistry meterRegistry) {
        this.ordersCreated = Counter.builder("orders.created")
                .description("Orders successfully committed")
                .register(meterRegistry);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void recordOrderCreated(OrderPlacedApplicationEvent event) {
        ordersCreated.increment();
    }
}
