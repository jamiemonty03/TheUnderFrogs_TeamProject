package com.neueda.orderservice.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.neueda.orderservice.models.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByAccountIdOrderByCreatedAtDesc(String accountId);
    boolean existsByIdempotencyKey(String idempotencyKey);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE orders
            SET order_status = :newStatus,
                last_updated = NOW(),
                version = version + 1,
                updated_by = 'SYSTEM'
            WHERE order_id = :orderId
              AND order_status = :expectedStatus
            """, nativeQuery = true)
    int updateStatusIfCurrent(@Param("orderId") String orderId,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("newStatus") String newStatus);
}
