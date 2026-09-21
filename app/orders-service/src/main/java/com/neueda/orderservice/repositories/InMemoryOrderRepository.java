package com.neueda.orderservice.repositories;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import com.neueda.orderservice.models.Order;

public class InMemoryOrderRepository implements OrderRepository {
    
    private final Map<String, Order> orders = new HashMap<>();

    @Override
    public Order save(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        
        orders.put(order.getOrderId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public List<Order> findByAccountId(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return List.of();
        }
        return orders.values().stream()
            .filter(order -> accountId.equals(order.getAccountId()))
            .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return false;
        }
        return orders.remove(orderId) != null;
    }

    @Override
    public boolean exists(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return false;
        }
        return orders.containsKey(orderId);
    }

    public void clear() {
        orders.clear();
    }

    public int count() {
        return orders.size();
    }
}
