package com.neueda.orderservice.dtos.requests;

import java.math.BigDecimal;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.enums.OrderStatus;

/**
 * DTO for updating mutable order fields.
 * 
 * All fields are optional (can be null). Only non-null fields will be updated.
 * Immutable fields (orderId, accountId, symbol, idempotencyKey, createdAt) cannot be changed.
 */
public record UpdateOrderRequest(
    Integer quantity,
    BigDecimal price,
    OrderSide side,
    OrderStatus orderStatus,
    String updatedBy
) {}
