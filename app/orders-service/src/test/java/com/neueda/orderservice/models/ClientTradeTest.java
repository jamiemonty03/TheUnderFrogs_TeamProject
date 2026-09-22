package com.neueda.orderservice.models;

import com.neueda.orderservice.enums.OrderSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClientTrade Entity Tests")
public class ClientTradeTest {

    @Test
    @DisplayName("Valid ClientTrade creation with all required fields")
    void testValidClientTradeCreation() {
        String tradeId = "TRADE001";
        String accountId = "ACC001";
        String symbol = "AAPL";
        OrderSide tradeType = OrderSide.BUY;
        BigDecimal quantity = new BigDecimal("100");
        BigDecimal price = new BigDecimal("150.25");
        LocalDateTime tradeDate = LocalDateTime.now();

        ClientTrade trade = new ClientTrade(tradeId, accountId, symbol, tradeType, quantity, price, tradeDate);

        assertEquals(tradeId, trade.getTradeId());
        assertEquals(accountId, trade.getAccountId());
        assertEquals(symbol, trade.getSymbol());
        assertEquals(tradeType, trade.getTradeType());
        assertEquals(quantity, trade.getQuantity());
        assertEquals(price, trade.getPrice());
        assertEquals(tradeDate, trade.getTradeDate());
        assertNotNull(trade.getCreatedAt());
    }

    @Test
    @DisplayName("ClientTrade creation sets createdAt to current time")
    void testCreatedAtIsSetToNow() {
        LocalDateTime beforeCreation = LocalDateTime.now();
        
        ClientTrade trade = new ClientTrade(
            "TRADE001",
            "ACC001",
            "AAPL",
            OrderSide.BUY,
            new BigDecimal("100"),
            new BigDecimal("150.25"),
            LocalDateTime.now()
        );

        LocalDateTime afterCreation = LocalDateTime.now();
        
        assertNotNull(trade.getCreatedAt());
        assertTrue(trade.getCreatedAt().isAfter(beforeCreation) || trade.getCreatedAt().isEqual(beforeCreation));
        assertTrue(trade.getCreatedAt().isBefore(afterCreation) || trade.getCreatedAt().isEqual(afterCreation));
    }

    @Test
    @DisplayName("Null tradeId throws IllegalArgumentException")
    void testNullTradeIdThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade(null, "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150.25"), LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Empty tradeId throws IllegalArgumentException")
    void testEmptyTradeIdThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("  ", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150.25"), LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Null accountId throws IllegalArgumentException")
    void testNullAccountIdThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("TRADE001", null, "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150.25"), LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Empty accountId throws IllegalArgumentException")
    void testEmptyAccountIdThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("TRADE001", "  ", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150.25"), LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Null symbol throws IllegalArgumentException")
    void testNullSymbolThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("TRADE001", "ACC001", null, OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150.25"), LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Empty symbol throws IllegalArgumentException")
    void testEmptySymbolThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("TRADE001", "ACC001", "  ", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150.25"), LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Null tradeType throws IllegalArgumentException")
    void testNullTradeTypeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("TRADE001", "ACC001", "AAPL", null, new BigDecimal("100"), new BigDecimal("150.25"), LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Zero quantity throws IllegalArgumentException")
    void testZeroQuantityThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("TRADE001", "ACC001", "AAPL", OrderSide.BUY, BigDecimal.ZERO, new BigDecimal("150.25"), LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Negative quantity throws IllegalArgumentException")
    void testNegativeQuantityThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("TRADE001", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("-100"), new BigDecimal("150.25"), LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Null quantity throws IllegalArgumentException")
    void testNullQuantityThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("TRADE001", "ACC001", "AAPL", OrderSide.BUY, null, new BigDecimal("150.25"), LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Price below 0.01 throws IllegalArgumentException")
    void testPriceTooLowThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("TRADE001", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("0.001"), LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Zero price throws IllegalArgumentException")
    void testZeroPriceThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("TRADE001", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), BigDecimal.ZERO, LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Null price throws IllegalArgumentException")
    void testNullPriceThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("TRADE001", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), null, LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Null tradeDate throws IllegalArgumentException")
    void testNullTradeDateThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTrade("TRADE001", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150.25"), null)
        );
    }

    @Test
    @DisplayName("No-arg constructor creates empty ClientTrade")
    void testNoArgConstructor() {
        ClientTrade trade = new ClientTrade();
        assertNull(trade.getTradeId());
        assertNull(trade.getAccountId());
        assertNull(trade.getSymbol());
        assertNull(trade.getTradeType());
        assertNull(trade.getQuantity());
        assertNull(trade.getPrice());
        assertNull(trade.getTradeDate());
        assertNull(trade.getCreatedAt());
    }

    @Test
    @DisplayName("Getter and setter methods work correctly")
    void testGettersAndSetters() {
        ClientTrade trade = new ClientTrade();
        
        trade.setTradeId("TRADE001");
        trade.setAccountId("ACC001");
        trade.setSymbol("AAPL");
        trade.setTradeType(OrderSide.SELL);
        trade.setQuantity(new BigDecimal("50"));
        trade.setPrice(new BigDecimal("160.50"));
        LocalDateTime tradeDate = LocalDateTime.now();
        trade.setTradeDate(tradeDate);
        LocalDateTime createdAt = LocalDateTime.now();
        trade.setCreatedAt(createdAt);

        assertEquals("TRADE001", trade.getTradeId());
        assertEquals("ACC001", trade.getAccountId());
        assertEquals("AAPL", trade.getSymbol());
        assertEquals(OrderSide.SELL, trade.getTradeType());
        assertEquals(new BigDecimal("50"), trade.getQuantity());
        assertEquals(new BigDecimal("160.50"), trade.getPrice());
        assertEquals(tradeDate, trade.getTradeDate());
        assertEquals(createdAt, trade.getCreatedAt());
    }

    @Test
    @DisplayName("SELL trade can be created")
    void testSellTradeCreation() {
        ClientTrade trade = new ClientTrade(
            "TRADE002",
            "ACC001",
            "AAPL",
            OrderSide.SELL,
            new BigDecimal("50"),
            new BigDecimal("160.00"),
            LocalDateTime.now()
        );

        assertEquals(OrderSide.SELL, trade.getTradeType());
    }

    @Test
    @DisplayName("Large quantity values are accepted")
    void testLargeQuantity() {
        ClientTrade trade = new ClientTrade(
            "TRADE003",
            "ACC001",
            "AAPL",
            OrderSide.BUY,
            new BigDecimal("999999999"),
            new BigDecimal("150.25"),
            LocalDateTime.now()
        );

        assertEquals(new BigDecimal("999999999"), trade.getQuantity());
    }

    @Test
    @DisplayName("High precision prices are accepted")
    void testHighPrecisionPrice() {
        ClientTrade trade = new ClientTrade(
            "TRADE004",
            "ACC001",
            "AAPL",
            OrderSide.BUY,
            new BigDecimal("100"),
            new BigDecimal("150.25999999"),
            LocalDateTime.now()
        );

        assertEquals(new BigDecimal("150.25999999"), trade.getPrice());
    }
}
