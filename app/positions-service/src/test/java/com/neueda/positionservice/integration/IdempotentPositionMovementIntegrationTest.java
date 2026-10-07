package com.neueda.positionservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.neueda.positionservice.exceptions.InsufficientHoldingsException;
import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.repositories.PositionRepository;
import com.neueda.positionservice.services.PositionService;

@Testcontainers
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class IdempotentPositionMovementIntegrationTest {

    private static final String ACCOUNT_ID = "ACC0001";
    private static final String SYMBOL = "AAPL";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void createSchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Stream<Path> files = Files.list(Path.of("db/schema"))) {
            for (Path file : files.sorted().toList()) {
                ScriptUtils.executeSqlScript(connection, new FileSystemResource(file));
            }
        }
    }

    @Autowired
    private PositionService positionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private PositionRepository positionRepository;

    @BeforeEach
    void seedPosition() {
        jdbcTemplate.update("DELETE FROM position_movements");
        jdbcTemplate.update("DELETE FROM positions");
        jdbcTemplate.update(
                "INSERT INTO positions (account_id, symbol, quantity, average_cost) VALUES (?, ?, 10, 100.0000)",
                ACCOUNT_ID, SYMBOL);
    }

    @Test
    @DisplayName("Buy twice with the same orderId changes the position once")
    void buyIsAppliedOncePerOrder() {
        positionService.updatePositionAfterBuy(ACCOUNT_ID, SYMBOL, 5, new BigDecimal("110.00"), "ORD-1");
        Position second = positionService.updatePositionAfterBuy(ACCOUNT_ID, SYMBOL, 5, new BigDecimal("110.00"), "ORD-1");

        assertDecimal("15", second.getQuantity());
        assertPosition("15", "103.3333");
        assertEquals(1, movements("ORD-1"));
    }

    @Test
    @DisplayName("Sell twice with the same orderId changes the position once")
    void sellIsAppliedOncePerOrder() throws Exception {
        positionService.updatePositionAfterSell(ACCOUNT_ID, SYMBOL, 4, "ORD-2");
        Position second = positionService.updatePositionAfterSell(ACCOUNT_ID, SYMBOL, 4, "ORD-2");

        assertDecimal("6", second.getQuantity());
        assertPosition("6", "100.0000");
        assertEquals(1, movements("ORD-2"));
    }

    @Test
    @DisplayName("Without an orderId every call is applied, as before")
    void sellWithoutOrderIdKeepsOldBehaviour() throws Exception {
        positionService.updatePositionAfterSell(ACCOUNT_ID, SYMBOL, 2);
        positionService.updatePositionAfterSell(ACCOUNT_ID, SYMBOL, 2);

        assertPosition("6", "100.0000");
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM position_movements", Integer.class));
    }

    @Test
    @DisplayName("Reversing a buy removes its shares and cost once")
    void reversalOfBuyRestoresPosition() throws Exception {
        positionService.updatePositionAfterBuy(ACCOUNT_ID, SYMBOL, 5, new BigDecimal("110.00"), "ORD-3");

        positionService.reverse(ACCOUNT_ID, SYMBOL, "ORD-3");
        positionService.reverse(ACCOUNT_ID, SYMBOL, "ORD-3");

        assertPosition("10", "100.0000");
        assertEquals(2, movements("ORD-3"));
    }

    @Test
    @DisplayName("Reversing a sell that emptied the position recreates it at the original average cost")
    void reversalOfFullSellRecreatesPosition() throws Exception {
        positionService.updatePositionAfterSell(ACCOUNT_ID, SYMBOL, 10, "ORD-4");
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM positions", Integer.class));

        positionService.reverse(ACCOUNT_ID, SYMBOL, "ORD-4");
        positionService.reverse(ACCOUNT_ID, SYMBOL, "ORD-4");

        assertPosition("10", "100.0000");
        assertEquals(2, movements("ORD-4"));
    }

    @Test
    @DisplayName("Reversing an order with no position movement changes nothing")
    void reversalWithoutOriginalChangesNothing() throws Exception {
        positionService.reverse(ACCOUNT_ID, SYMBOL, "ORD-NEVER-TRADED");

        assertPosition("10", "100.0000");
        assertEquals(0, movements("ORD-NEVER-TRADED"));
    }

    @Test
    @DisplayName("A failed check after the ledger insert rolls the ledger row back, so a retry still works")
    void insufficientHoldingsRollsBackLedgerRow() throws Exception {
        assertThrows(InsufficientHoldingsException.class,
                () -> positionService.updatePositionAfterSell(ACCOUNT_ID, SYMBOL, 20, "ORD-5"));
        assertEquals(0, movements("ORD-5"));

        positionService.updatePositionAfterBuy(ACCOUNT_ID, SYMBOL, 20, new BigDecimal("100.00"));
        positionService.updatePositionAfterSell(ACCOUNT_ID, SYMBOL, 20, "ORD-5");

        assertPosition("10", "100.0000");
        assertEquals(1, movements("ORD-5"));
    }

    @Test
    @DisplayName("A forced failure after the ledger insert rolls back both the ledger row and the position")
    void forcedFailureRollsBackBothWrites() {
        doThrow(new RuntimeException("forced failure")).when(positionRepository).save(any(Position.class));

        assertThrows(RuntimeException.class,
                () -> positionService.updatePositionAfterBuy(ACCOUNT_ID, SYMBOL, 5, new BigDecimal("110.00"), "ORD-6"));

        assertPosition("10", "100.0000");
        assertEquals(0, movements("ORD-6"));
    }

    private void assertPosition(String expectedQuantity, String expectedAverageCost) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT quantity, average_cost FROM positions WHERE account_id = ? AND symbol = ?",
                ACCOUNT_ID, SYMBOL);
        assertEquals(1, rows.size(), "expected exactly one position row");
        assertDecimal(expectedQuantity, (BigDecimal) rows.get(0).get("quantity"));
        assertDecimal(expectedAverageCost, (BigDecimal) rows.get(0).get("average_cost"));
    }

    private int movements(String orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM position_movements WHERE order_id = ?", Integer.class, orderId);
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }
}
