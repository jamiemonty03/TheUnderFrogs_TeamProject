package com.neueda.leap.services;

import com.neueda.leap.models.Order;
import com.neueda.leap.exceptions.DuplicateOrderException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for creating orders with idempotency support.
 * Prevents duplicate order processing by tracking idempotency keys.
 */
public class OrderService {
    
    // Track orders by idempotency key to detect duplicates
    private final Map<String, Order> ordersByIdempotencyKey = new ConcurrentHashMap<>();

    public Order createOrder(Order order) throws DuplicateOrderException {
        if (order.getIdempotencyKey() == null || order.getIdempotencyKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        
        // Check for duplicate idempotency key
        if (ordersByIdempotencyKey.containsKey(order.getIdempotencyKey())) {
            throw new DuplicateOrderException(order.getIdempotencyKey(), "idempotencyKey");
        }
        
        // Store order with idempotency key
        ordersByIdempotencyKey.put(order.getIdempotencyKey(), order);
        return order;
    }

    public Optional<Order> getOrderByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(ordersByIdempotencyKey.get(idempotencyKey));
    }
}
