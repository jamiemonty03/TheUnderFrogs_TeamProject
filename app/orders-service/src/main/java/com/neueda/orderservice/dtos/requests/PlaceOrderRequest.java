package com.neueda.orderservice.dtos.requests;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import com.neueda.orderservice.enums.OrderSide;

public record PlaceOrderRequest(
    @NotBlank(message = "Account ID is required") String accountId,
    @NotBlank(message = "Symbol is required") String symbol,
    @NotNull(message = "Order side (BUY/SELL) is required") OrderSide side,
    @NotNull(message = "Quantity is required") @DecimalMin(value = "0.01", message = "Quantity must be greater than 0") BigDecimal quantity,
    @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be greater than 0") BigDecimal price,
    @NotBlank(message = "Idempotency key is required") String idempotencyKey
) {}
