package com.neueda.orderservice.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
 * trade_id is a SERIAL column, so the database assigns it on insert.
 *
 * @see ClientTradeService
 * @see Position
 */
@Entity
@Table(name = "client_trades")
public class ClientTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Integer tradeId;

    @NotNull(message = "AccountId is mandatory")
    @NotBlank(message = "AccountId cannot be blank")
    @Column(name = "account_id")
    private String accountId;

    @NotNull(message = "Symbol is mandatory")
    @NotBlank(message = "Symbol cannot be blank")
    private String symbol;

    @NotNull(message = "Trade type (BUY/SELL) is mandatory")
    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type")
    private OrderSide tradeType;

    @NotNull(message = "Quantity is mandatory")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Price is mandatory")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Trade date is mandatory")
    @Column(name = "trade_date")
    private LocalDate tradeDate;

    private int version;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "updated_by")
    private String updatedBy;


    public ClientTrade() {}

    public ClientTrade(String accountId, String symbol, OrderSide tradeType,
                      BigDecimal quantity, BigDecimal price, LocalDate tradeDate) {

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

        this.accountId = accountId;
        this.symbol = symbol;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.price = price;
        this.tradeDate = tradeDate;
        this.version = 0;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.updatedBy = "SYSTEM";
    }

    public Integer getTradeId() {
        return tradeId;
    }

    public void setTradeId(Integer tradeId) {
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

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
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

    @Override
    public String toString() {
        return "ClientTrade{" +
                "tradeId=" + tradeId +
                ", accountId='" + accountId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", tradeType=" + tradeType +
                ", quantity=" + quantity +
                ", price=" + price +
                ", tradeDate=" + tradeDate +
                '}';
    }
}
