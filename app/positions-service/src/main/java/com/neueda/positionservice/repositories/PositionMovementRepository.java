package com.neueda.positionservice.repositories;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neueda.positionservice.models.PositionMovement;

public interface PositionMovementRepository extends JpaRepository<PositionMovement, Integer> {

    @Modifying
    @Query(value = """
            INSERT INTO position_movements (order_id, movement_type, account_id, symbol, quantity, price)
            VALUES (:orderId, :movementType, :accountId, :symbol, :quantity, :price)
            ON CONFLICT (order_id, movement_type) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("orderId") String orderId,
                       @Param("movementType") String movementType,
                       @Param("accountId") String accountId,
                       @Param("symbol") String symbol,
                       @Param("quantity") BigDecimal quantity,
                       @Param("price") BigDecimal price);

    List<PositionMovement> findByOrderId(String orderId);
}
