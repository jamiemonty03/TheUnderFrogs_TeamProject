package com.neueda.leap.repositories;

import java.util.Optional;
import java.util.List;
import com.neueda.leap.models.Order;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String orderId);
    List<Order> findByAccountId(String accountId);
    boolean delete(String orderId);
    boolean exists(String orderId);
}
