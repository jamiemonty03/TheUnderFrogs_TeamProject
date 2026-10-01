package com.neueda.orderservice.services;

import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.exceptions.OrderNotFoundException;
import com.neueda.orderservice.exceptions.OrderStatusConflictException;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.repositories.OrderRepository;

@Service
public class OrderStatusService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusService.class);
    private static final Set<OrderStatus> ALLOWED_FROM_NEW =
            EnumSet.of(OrderStatus.FILLED, OrderStatus.REJECTED, OrderStatus.CANCELLED);

    private final OrderRepository orderRepository;

    public OrderStatusService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order changeStatus(String orderId, OrderStatus expectedStatus, OrderStatus newStatus, String reason)
            throws OrderNotFoundException, OrderStatusConflictException {
        if (expectedStatus != OrderStatus.NEW || !ALLOWED_FROM_NEW.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Status change " + expectedStatus + " -> " + newStatus + " is not allowed");
        }

        int updated = orderRepository.updateStatusIfCurrent(orderId, expectedStatus.name(), newStatus.name());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (updated == 0) {
            throw new OrderStatusConflictException(orderId, expectedStatus, order.getOrderStatus());
        }

        log.info("Order {} status {} -> {} ({})", orderId, expectedStatus, newStatus, reason);
        return order;
    }
}
