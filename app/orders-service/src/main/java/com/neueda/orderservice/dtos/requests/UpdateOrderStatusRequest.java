package com.neueda.orderservice.dtos.requests;

import java.math.BigDecimal;

import com.neueda.orderservice.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
    @NotNull(message = "expectedStatus is required") OrderStatus expectedStatus,
    @NotNull(message = "newStatus is required") OrderStatus newStatus,
    BigDecimal fillPrice,
    String reason
) {}
