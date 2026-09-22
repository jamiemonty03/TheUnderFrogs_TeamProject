package com.neueda.orderservice.repositories;

import com.neueda.orderservice.models.ClientTrade;
import com.neueda.orderservice.enums.OrderSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryClientTradeRepository Tests")
public class InMemoryClientTradeRepositoryTest {

    private InMemoryClientTradeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryClientTradeRepository();
    }

    @Test
    @DisplayName("Save and retrieve a trade by ID")
    void testSaveAndFindById() {
        ClientTrade trade = new ClientTrade("T1", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDateTime.now());
        
        repository.save(trade);
        Optional<ClientTrade> result = repository.findById("T1");

        assertTrue(result.isPresent());
        assertEquals("T1", result.get().getTradeId());
        assertEquals("AAPL", result.get().getSymbol());
    }

    @Test
    @DisplayName("Find by ID returns empty when trade doesn't exist")
    void testFindByIdNotFound() {
        Optional<ClientTrade> result = repository.findById("NONEXISTENT");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Find by ID throws for null ID")
    void testFindByIdNull() {
        assertThrows(IllegalArgumentException.class, () -> repository.findById(null));
    }

    @Test
    @DisplayName("Find by ID throws for blank ID")
    void testFindByIdBlank() {
        assertThrows(IllegalArgumentException.class, () -> repository.findById("  "));
    }

    @Test
    @DisplayName("Find all trades for account")
    void testFindByAccountId() {
        ClientTrade trade1 = new ClientTrade("T1", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDateTime.now());
        ClientTrade trade2 = new ClientTrade("T2", "ACC001", "MSFT", OrderSide.SELL, new BigDecimal("50"), new BigDecimal("300"), LocalDateTime.now());
        ClientTrade trade3 = new ClientTrade("T3", "ACC002", "AAPL", OrderSide.BUY, new BigDecimal("75"), new BigDecimal("155"), LocalDateTime.now());

        repository.save(trade1);
        repository.save(trade2);
        repository.save(trade3);

        List<ClientTrade> result = repository.findByAccountId("ACC001");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.getAccountId().equals("ACC001")));
    }

    @Test
    @DisplayName("Find by account ID returns empty for unknown account")
    void testFindByAccountIdEmpty() {
        List<ClientTrade> result = repository.findByAccountId("UNKNOWN");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Find by account ID throws for null account ID")
    void testFindByAccountIdNull() {
        assertThrows(IllegalArgumentException.class, () -> repository.findByAccountId(null));
    }

    @Test
    @DisplayName("Find by account ID throws for blank account ID")
    void testFindByAccountIdBlank() {
        assertThrows(IllegalArgumentException.class, () -> repository.findByAccountId("  "));
    }

    @Test
    @DisplayName("Find trades for account and symbol")
    void testFindByAccountAndSymbol() {
        ClientTrade trade1 = new ClientTrade("T1", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDateTime.now());
        ClientTrade trade2 = new ClientTrade("T2", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("50"), new BigDecimal("155"), LocalDateTime.now());
        ClientTrade trade3 = new ClientTrade("T3", "ACC001", "MSFT", OrderSide.SELL, new BigDecimal("30"), new BigDecimal("300"), LocalDateTime.now());

        repository.save(trade1);
        repository.save(trade2);
        repository.save(trade3);

        List<ClientTrade> result = repository.findByAccountAndSymbol("ACC001", "AAPL");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.getSymbol().equals("AAPL")));
    }

    @Test
    @DisplayName("Find by account and symbol returns empty for unknown pair")
    void testFindByAccountAndSymbolEmpty() {
        List<ClientTrade> result = repository.findByAccountAndSymbol("ACC001", "UNKNOWN");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Find by account and symbol throws for null account ID")
    void testFindByAccountAndSymbolNullAccountId() {
        assertThrows(IllegalArgumentException.class, () -> repository.findByAccountAndSymbol(null, "AAPL"));
    }

    @Test
    @DisplayName("Find by account and symbol throws for null symbol")
    void testFindByAccountAndSymbolNullSymbol() {
        assertThrows(IllegalArgumentException.class, () -> repository.findByAccountAndSymbol("ACC001", null));
    }

    @Test
    @DisplayName("Find by account and symbol throws for blank account ID")
    void testFindByAccountAndSymbolBlankAccountId() {
        assertThrows(IllegalArgumentException.class, () -> repository.findByAccountAndSymbol("  ", "AAPL"));
    }

    @Test
    @DisplayName("Find by account and symbol throws for blank symbol")
    void testFindByAccountAndSymbolBlankSymbol() {
        assertThrows(IllegalArgumentException.class, () -> repository.findByAccountAndSymbol("ACC001", "  "));
    }

    @Test
    @DisplayName("Delete removes a trade by ID")
    void testDelete() {
        ClientTrade trade = new ClientTrade("T1", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDateTime.now());
        repository.save(trade);

        boolean result = repository.delete("T1");

        assertTrue(result);
        assertFalse(repository.findById("T1").isPresent());
    }

    @Test
    @DisplayName("Delete returns false for nonexistent trade")
    void testDeleteNonexistent() {
        boolean result = repository.delete("NONEXISTENT");

        assertFalse(result);
    }

    @Test
    @DisplayName("Delete throws for null ID")
    void testDeleteNull() {
        assertThrows(IllegalArgumentException.class, () -> repository.delete(null));
    }

    @Test
    @DisplayName("Delete throws for blank ID")
    void testDeleteBlank() {
        assertThrows(IllegalArgumentException.class, () -> repository.delete("  "));
    }

    @Test
    @DisplayName("Exists returns true for existing trade")
    void testExistsTrue() {
        ClientTrade trade = new ClientTrade("T1", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDateTime.now());
        repository.save(trade);

        boolean result = repository.exists("T1");

        assertTrue(result);
    }

    @Test
    @DisplayName("Exists returns false for nonexistent trade")
    void testExistsFalse() {
        boolean result = repository.exists("NONEXISTENT");

        assertFalse(result);
    }

    @Test
    @DisplayName("Exists throws for null ID")
    void testExistsNull() {
        assertThrows(IllegalArgumentException.class, () -> repository.exists(null));
    }

    @Test
    @DisplayName("Exists throws for blank ID")
    void testExistsBlank() {
        assertThrows(IllegalArgumentException.class, () -> repository.exists("  "));
    }

    @Test
    @DisplayName("Save throws for null trade")
    void testSaveNull() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    @Test
    @DisplayName("Save throws for trade with null ID")
    void testSaveNullTradeId() {
        ClientTrade trade = new ClientTrade();
        trade.setAccountId("ACC001");
        trade.setSymbol("AAPL");

        assertThrows(IllegalArgumentException.class, () -> repository.save(trade));
    }

    @Test
    @DisplayName("Save throws for trade with blank ID")
    void testSaveBlankTradeId() {
        ClientTrade trade = new ClientTrade();
        trade.setTradeId("  ");
        trade.setAccountId("ACC001");
        trade.setSymbol("AAPL");

        assertThrows(IllegalArgumentException.class, () -> repository.save(trade));
    }

    @Test
    @DisplayName("Clear removes all trades")
    void testClear() {
        ClientTrade trade1 = new ClientTrade("T1", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDateTime.now());
        ClientTrade trade2 = new ClientTrade("T2", "ACC001", "MSFT", OrderSide.SELL, new BigDecimal("50"), new BigDecimal("300"), LocalDateTime.now());

        repository.save(trade1);
        repository.save(trade2);
        assertEquals(2, repository.size());

        repository.clear();

        assertEquals(0, repository.size());
    }

    @Test
    @DisplayName("Size returns correct number of trades")
    void testSize() {
        assertEquals(0, repository.size());

        ClientTrade trade1 = new ClientTrade("T1", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDateTime.now());
        repository.save(trade1);
        assertEquals(1, repository.size());

        ClientTrade trade2 = new ClientTrade("T2", "ACC001", "MSFT", OrderSide.SELL, new BigDecimal("50"), new BigDecimal("300"), LocalDateTime.now());
        repository.save(trade2);
        assertEquals(2, repository.size());

        repository.delete("T1");
        assertEquals(1, repository.size());
    }

    @Test
    @DisplayName("Update existing trade by saving with same ID")
    void testUpdateTrade() {
        ClientTrade trade = new ClientTrade("T1", "ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDateTime.now());
        repository.save(trade);

        trade.setQuantity(new BigDecimal("200"));
        repository.save(trade);

        Optional<ClientTrade> result = repository.findById("T1");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("200"), result.get().getQuantity());
    }
}
