package com.neueda.orderservice.dtos.requests;

import java.math.BigDecimal;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.enums.OrderStatus;

import jakarta.validation.constraints.Null;

/**
 * DTO for updating mutable order fields.
 *
 * All fields are optional (can be null). Only non-null fields will be updated.
 * Immutable fields (orderId, accountId, symbol, idempotencyKey, createdAt) cannot be changed.
 * orderStatus must be left empty: status only changes through PATCH /orders/{id}/status.
 */
public record UpdateOrderRequest(
    Integer quantity,
    BigDecimal priceLimit,
    OrderSide side,
    @Null(message = "orderStatus cannot be changed with PUT; use PATCH /orders/{id}/status")
    OrderStatus orderStatus,
    String updatedBy
) {}
