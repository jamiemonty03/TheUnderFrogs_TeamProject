package com.neueda.accountservice.repositories;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neueda.accountservice.enums.MovementType;
import com.neueda.accountservice.models.CashMovement;

public interface CashMovementRepository extends JpaRepository<CashMovement, Integer> {

    @Modifying
    @Query(value = """
            INSERT INTO cash_movements (order_id, movement_type, account_id, amount)
            VALUES (:orderId, :movementType, :accountId, :amount)
            ON CONFLICT (order_id, movement_type) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("orderId") String orderId,
                       @Param("movementType") String movementType,
                       @Param("accountId") String accountId,
                       @Param("amount") BigDecimal amount);

    List<CashMovement> findByOrderId(String orderId);

}
