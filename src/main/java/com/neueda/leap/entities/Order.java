package com.neueda.leap.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;

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

    /**
     * Order direction enumeration.
     * BUY: Acquire position (debit account cash, create/increase position)
     * SELL: Liquidate position (credit account cash, decrease position)
     */
    public enum Side { BUY, SELL }
    
    /**
     * Order status enumeration.
     * NEW: Order created, awaiting processing
     * FILLED: Order successfully executed
     * REJECTED: Order failed validation (insufficient funds, holdings, etc.)
     * CANCELLED: Order cancelled by user
     */
    public enum Status { NEW, FILLED, REJECTED, CANCELLED }

    private String orderId;
    
    @NotNull(message = "AccountId is mandatory")
    private String accountId;
    
    @NotBlank(message = "Symbol is mandatory")
    private String symbol;
    
    @NotNull(message = "Order side (BUY/SELL) is mandatory")
    private Side side;
    
    @Positive(message = "Quantity must be positive")
    private int quantity;
    
    @NotNull(message = "Price is mandatory")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
    
    @NotNull(message = "Idempotency key is mandatory")
    @NotBlank(message = "Idempotency key cannot be blank")
    private String idempotencyKey;
    
    @NotNull(message = "Order status is mandatory")
    private Status orderStatus;
    
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String updatedBy;


    public Order() {}

    public Order(String orderId, String accountId, String symbol, Side side, int quantity, BigDecimal price, String idempotencyKey) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.idempotencyKey = idempotencyKey;
        this.orderStatus = Status.NEW; 
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

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
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

    public Status getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Status orderStatus) {
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

    /**
     * Get the idempotency key for this order.
     * Prevents duplicate order processing.
     * @return the unique idempotency key
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * Set the idempotency key for this order.
     * @param idempotencyKey the unique idempotency key
     */
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
