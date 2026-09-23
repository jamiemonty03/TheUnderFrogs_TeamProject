package com.neueda.orderservice.repositories;

import java.util.Optional;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.orderservice.models.Order;

@Mapper
public interface OrderRepository {
    void save(Order order);
    int update(Order order);
    List<Order> findAll();
    Optional<Order> findById(String orderId);
    List<Order> findByAccountId(String accountId);
    void delete(String orderId);
    boolean exists(String orderId);
}
