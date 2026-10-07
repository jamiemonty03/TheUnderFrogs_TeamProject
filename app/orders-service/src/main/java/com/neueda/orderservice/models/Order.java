package com.neueda.orderservice.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.enums.OrderStatus;

/**
 * Represents a trade order (buy or sell) for a financial instrument.
 * 
 * Tracks the complete lifecycle of an order from creation through execution:
 * - Order submission with quantity, price limit, and direction (buy/sell)
 * - Status transitions (NEW → FILLED or REJECTED)
 * - Version tracking for optimistic locking during concurrent updates
 * 
 * priceLimit is the worst price per share the user accepts; the trade-executor
 * fills at the market price, or rejects the order if the market is past the limit.
 * 
 * @see Account
 * @see Position
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @NotNull(message = "AccountId is mandatory")
    @Column(name = "account_id")
    private String accountId;

    @NotBlank(message = "Symbol is mandatory")
    private String symbol;

    @NotNull(message = "Order side (BUY/SELL) is mandatory")
    @Enumerated(EnumType.STRING)
    private OrderSide side;

    @Positive(message = "Quantity must be positive")
    private int quantity;

    @NotNull(message = "Price limit is mandatory")
    @DecimalMin(value = "0.01", message = "Price limit must be greater than 0")
    @Column(name = "price_limit")
    private BigDecimal priceLimit;

    @NotNull(message = "Idempotency key is mandatory")
    @NotBlank(message = "Idempotency key cannot be blank")
    @Column(name = "idempotency_key", updatable = false)
    private String idempotencyKey;

    @NotNull(message = "Order status is mandatory")
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;

    private int version;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "updated_by")
    private String updatedBy;


    public Order() {}

    public Order(String orderId, String accountId, String symbol, OrderSide side, int quantity, BigDecimal priceLimit, String idempotencyKey) {
        if (accountId == null) {
        throw new IllegalArgumentException("AccountId is mandatory");
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol is mandatory");
        }
        if (side == null) {
            throw new IllegalArgumentException("Order side (BUY/SELL) is mandatory");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (priceLimit == null) {
            throw new IllegalArgumentException("Price limit is mandatory");
        }
        if (priceLimit.compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("Price limit must be greater than 0");
        }
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key is mandatory and cannot be blank");
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is mandatory and cannot be blank");
        }
        
        this.orderId = orderId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.priceLimit = priceLimit;
        this.idempotencyKey = idempotencyKey;
        this.orderStatus = OrderStatus.NEW; 
        this.version = 0;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.updatedBy = "SYSTEM";
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPriceLimit() {
        return priceLimit;
    }

    public void setPriceLimit(BigDecimal priceLimit) {
        this.priceLimit = priceLimit;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", side=" + side +
                ", quantity=" + quantity +
                ", priceLimit=" + priceLimit +
                ", orderStatus=" + orderStatus +
                ", version=" + version +
                '}';
    }
}
