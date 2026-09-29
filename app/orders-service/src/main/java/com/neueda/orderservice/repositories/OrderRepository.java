package com.neueda.orderservice.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.neueda.orderservice.models.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByAccountIdOrderByCreatedAtDesc(String accountId);
    boolean existsByIdempotencyKey(String idempotencyKey);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
