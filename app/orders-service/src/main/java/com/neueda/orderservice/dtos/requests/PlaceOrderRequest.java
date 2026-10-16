package com.neueda.orderservice.dtos.requests;

import java.math.BigDecimal;

import com.neueda.orderservice.enums.OrderSide;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceOrderRequest(
    @NotBlank(message = "Account ID is required") String accountId,
    @NotBlank(message = "Symbol is required") String symbol,
    @NotNull(message = "Order side (BUY/SELL) is required") OrderSide side,
    @NotNull(message = "Quantity is required") @DecimalMin(value = "0.01", message = "Quantity must be greater than 0") BigDecimal quantity,
    @Schema(description = "Worst price per share you accept: the most you pay on a BUY, the least you receive on a SELL. "
            + "The order fills at the market price, or is rejected if the market is past this limit.", example = "190.00")
    @NotNull(message = "Price limit is required") @DecimalMin(value = "0.01", message = "Price limit must be greater than 0") BigDecimal priceLimit,
    @NotBlank(message = "Idempotency key is required") String idempotencyKey,
    @Schema(description = "Optional. The portfolio to buy into or sell from. Without it, the account's Main portfolio.",
            example = "PF0012")
    @Size(max = 32, message = "Portfolio ID must be 32 characters or fewer") String portfolioId
) {
    public PlaceOrderRequest(String accountId, String symbol, OrderSide side, BigDecimal quantity,
                             BigDecimal priceLimit, String idempotencyKey) {
        this(accountId, symbol, side, quantity, priceLimit, idempotencyKey, null);
    }
}
