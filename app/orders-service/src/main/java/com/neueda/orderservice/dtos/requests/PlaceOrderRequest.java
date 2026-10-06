package com.neueda.orderservice.dtos.requests;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import io.swagger.v3.oas.annotations.media.Schema;
import com.neueda.orderservice.enums.OrderSide;

public record PlaceOrderRequest(
    @NotBlank(message = "Account ID is required") String accountId,
    @NotBlank(message = "Symbol is required") String symbol,
    @NotNull(message = "Order side (BUY/SELL) is required") OrderSide side,
    @NotNull(message = "Quantity is required") @DecimalMin(value = "0.01", message = "Quantity must be greater than 0") BigDecimal quantity,
    @Schema(description = "Worst price per share you accept: the most you pay on a BUY, the least you receive on a SELL. "
            + "The order fills at the market price, or is rejected if the market is past this limit.", example = "190.00")
    @NotNull(message = "Price limit is required") @DecimalMin(value = "0.01", message = "Price limit must be greater than 0") BigDecimal priceLimit,
    @NotBlank(message = "Idempotency key is required") String idempotencyKey
) {}
