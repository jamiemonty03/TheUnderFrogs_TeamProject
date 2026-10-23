package com.neueda.tradeexecutor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import com.neueda.tradeexecutor.clients.OrdersClient;
import com.neueda.tradeexecutor.dtos.OrderDto;
import com.neueda.tradeexecutor.events.PriceChanged;

/** Re-checks resting limit orders whenever their symbol's price changes. */
@Service
public class RestingOrderMatcher {

    private static final Logger log = LoggerFactory.getLogger(RestingOrderMatcher.class);

    private final OrdersClient ordersClient;
    private final OrderExecutionService executionService;

    public RestingOrderMatcher(OrdersClient ordersClient, OrderExecutionService executionService) {
        this.ordersClient = ordersClient;
        this.executionService = executionService;
    }

    @EventListener
    public void onPriceChanged(PriceChanged event) {
        try {
            for (OrderDto order : ordersClient.getOpenOrders(event.symbol())) {
                recheck(order.orderId());
            }
        } catch (Exception e) {
            log.warn("Couldn't load open orders for {}, trying again on the next price: {}", event.symbol(), e.getMessage());
        }
    }

    // One failing order mustn't stop the rest, or the price poll that raised the event
    private void recheck(String orderId) {
        try {
            executionService.execute(orderId);
        } catch (Exception e) {
            log.warn("Re-checking order {} failed, trying again on the next price: {}", orderId, e.getMessage());
        }
    }
}
