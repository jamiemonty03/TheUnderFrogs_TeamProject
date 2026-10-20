package com.neueda.orderservice.services;

import com.neueda.orderservice.enums.OrderStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusMetrics {

    private final Map<OrderStatus, Counter> counters = new EnumMap<>(OrderStatus.class);
    private final Map<String, Counter> tradeCounters = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public OrderStatusMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        for (OrderStatus status : OrderStatus.values()) {
            counters.put(status, Counter.builder("orders.status")
                    .tag("status", status.name())
                    .description("Orders reaching each lifecycle status")
                    .register(meterRegistry));
        }
    }

    public void record(OrderStatus status) {
        counters.get(status).increment();
    }

    public void recordTradeExecuted(String symbol) {
        tradeCounters.computeIfAbsent(symbol, key -> Counter.builder("trades.executed")
                .tag("symbol", key)
                .description("Trades successfully executed by symbol")
                .register(meterRegistry)).increment();
    }
}
