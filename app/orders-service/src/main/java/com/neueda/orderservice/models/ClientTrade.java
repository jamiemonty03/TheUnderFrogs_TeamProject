package com.neueda.orderservice.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import com.neueda.orderservice.enums.OrderSide;

/**
 * Represents a completed trade from client data.
 * 
 * Tracks historical trade information imported from client_trades table,
 * providing a historical record of all trades executed within an account.
 * Used for reconciliation and weighted average cost calculations.
 * 
 * @see ClientTradeService
 * @see Position
 */
public class ClientTrade {

    private String tradeId;

    @NotNull(message = "AccountId is mandatory")
    @NotBlank(message = "AccountId cannot be blank")
    private String accountId;

    @NotNull(message = "Symbol is mandatory")
    @NotBlank(message = "Symbol cannot be blank")
    private String symbol;

    @NotNull(message = "Trade type (BUY/SELL) is mandatory")
    private OrderSide tradeType;

    @NotNull(message = "Quantity is mandatory")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Price is mandatory")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Trade date is mandatory")
    private LocalDateTime tradeDate;

    private LocalDateTime createdAt;


    public ClientTrade() {}

    public ClientTrade(String tradeId, String accountId, String symbol, OrderSide tradeType,
                      BigDecimal quantity, BigDecimal price, LocalDateTime tradeDate) {

        if (tradeId == null || tradeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Trade ID is mandatory and cannot be blank");
        }
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("AccountId is mandatory and cannot be blank");
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol is mandatory and cannot be blank");
        }
        if (tradeType == null) {
            throw new IllegalArgumentException("Trade type (BUY/SELL) is mandatory");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price == null || price.compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
        if (tradeDate == null) {
            throw new IllegalArgumentException("Trade date is mandatory");
        }

        this.tradeId = tradeId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.price = price;
        this.tradeDate = tradeDate;
        this.createdAt = LocalDateTime.now();
    }

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public OrderSide getTradeType() {
        return tradeType;
    }

    public void setTradeType(OrderSide tradeType) {
        this.tradeType = tradeType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDateTime tradeDate) {
        this.tradeDate = tradeDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
