package com.neueda.leap.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.enums.OrderStatus;

/**
 * Represents a trade order (buy or sell) for a financial instrument.
 * 
 * Tracks the complete lifecycle of an order from creation through execution:
 * - Order submission with quantity, price, and direction (buy/sell)
 * - Status transitions (NEW → FILLED or REJECTED)
 * - Version tracking for optimistic locking during concurrent updates
 * 
 * Total cost calculation: quantity × price
 * 
 * @see Account
 * @see Position
 */
public class Order {

    private String orderId;
    
    @NotNull(message = "AccountId is mandatory")
    private String accountId;
    
    @NotBlank(message = "Symbol is mandatory")
    private String symbol;
    
    @NotNull(message = "Order side (BUY/SELL) is mandatory")
    private OrderSide side;
    
    @Positive(message = "Quantity must be positive")
    private int quantity;
    
    @NotNull(message = "Price is mandatory")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
    
    @NotNull(message = "Idempotency key is mandatory")
    @NotBlank(message = "Idempotency key cannot be blank")
    private String idempotencyKey;
    
    @NotNull(message = "Order status is mandatory")
    private OrderStatus orderStatus;
    
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String updatedBy;


    public Order() {}

    public Order(String orderId, String accountId, String symbol, OrderSide side, int quantity, BigDecimal price, String idempotencyKey) {
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
        if (price == null) {
            throw new IllegalArgumentException("Price is mandatory");
        }
        if (price.compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
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
        this.price = price;
        this.idempotencyKey = idempotencyKey;
        this.orderStatus = OrderStatus.NEW; 
        this.version = 0;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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
                ", price=" + price +
                ", orderStatus=" + orderStatus +
                ", version=" + version +
                '}';
    }
}
