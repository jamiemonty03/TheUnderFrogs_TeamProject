package com.neueda.positionservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.neueda.positionservice.dtos.requests.CreatePortfolioRequest;
import com.neueda.positionservice.enums.PortfolioRisk;
import com.neueda.positionservice.exceptions.InsufficientHoldingsException;
import com.neueda.positionservice.exceptions.PortfolioConflictException;
import com.neueda.positionservice.exceptions.PortfolioNotFoundException;
import com.neueda.positionservice.models.Portfolio;
import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.services.PortfolioService;
import com.neueda.positionservice.services.PositionService;

@Testcontainers
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class PortfolioIntegrationTest {

    private static final String ACCOUNT_ID = "ACC0011";
    private static final String OTHER_ACCOUNT_ID = "ACC0001";
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
    private PortfolioService portfolioService;

    @Autowired
    private PositionService positionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void emptyTables() {
        jdbcTemplate.update("DELETE FROM position_movements");
        jdbcTemplate.update("DELETE FROM positions");
        jdbcTemplate.update("DELETE FROM portfolios");
    }

    @Test
    @DisplayName("Main is created once, the first time an account needs it, with a PF id")
    void mainIsCreatedOnce() {
        Portfolio first = portfolioService.resolve(ACCOUNT_ID, null);
        Portfolio second = portfolioService.resolve(ACCOUNT_ID, null);

        assertEquals(first.getPortfolioId(), second.getPortfolioId());
        assertTrue(first.getPortfolioId().matches("PF\\d{4,}"), first.getPortfolioId());
        assertTrue(first.isDefaultPortfolio());
        assertEquals("Main", first.getName());
        assertEquals(1, count("SELECT COUNT(*) FROM portfolios WHERE account_id = ?", ACCOUNT_ID));
    }

    @Test
    @DisplayName("A buy without a portfolio goes to Main, as the trade-executor sends today")
    void buyWithoutPortfolioGoesToMain() {
        Position bought = positionService.updatePositionAfterBuy(ACCOUNT_ID, null, SYMBOL, 10,
            new BigDecimal("100.00"), "ORD-1");

        Portfolio main = portfolioService.resolve(ACCOUNT_ID, null);
        assertEquals(main.getPortfolioId(), bought.getPortfolioId());
        assertEquals(main.getPortfolioId(), jdbcTemplate.queryForObject(
            "SELECT portfolio_id FROM position_movements WHERE order_id = 'ORD-1'", String.class));
    }

    @Test
    @DisplayName("The same symbol in two portfolios keeps separate quantities and average costs")
    void holdingsAreSeparatePerPortfolio() throws Exception {
        Portfolio growth = createGrowth();

        positionService.updatePositionAfterBuy(ACCOUNT_ID, null, SYMBOL, 10, new BigDecimal("100.00"), null);
        positionService.updatePositionAfterBuy(ACCOUNT_ID, growth.getPortfolioId(), SYMBOL, 4,
            new BigDecimal("150.00"), null);

        List<Position> all = positionService.getPositionsByAccountId(ACCOUNT_ID, null);
        List<Position> inGrowth = positionService.getPositionsByAccountId(ACCOUNT_ID, growth.getPortfolioId());

        assertEquals(2, all.size());
        assertEquals(1, inGrowth.size());
        assertDecimal("4", inGrowth.get(0).getQuantity());
        assertDecimal("150", inGrowth.get(0).getAverageCost());
        assertDecimal("10", positionService.getPosition(ACCOUNT_ID, null, SYMBOL).orElseThrow().getQuantity());

        // Selling from Growth can't use the shares held in Main
        assertThrows(InsufficientHoldingsException.class,
            () -> positionService.updatePositionAfterSell(ACCOUNT_ID, growth.getPortfolioId(), SYMBOL, 5, null));
    }

    @Test
    @DisplayName("Reversing an order undoes it in the portfolio the order went to")
    void reversalUsesTheOrdersPortfolio() throws Exception {
        Portfolio growth = createGrowth();
        positionService.updatePositionAfterBuy(ACCOUNT_ID, growth.getPortfolioId(), SYMBOL, 4,
            new BigDecimal("150.00"), "ORD-7");

        positionService.reverse(ACCOUNT_ID, SYMBOL, "ORD-7");

        assertEquals(0, count("SELECT COUNT(*) FROM positions WHERE portfolio_id = ?", growth.getPortfolioId()));
    }

    @Test
    @DisplayName("Another account's portfolio can't be used, even with its id")
    void anotherAccountsPortfolioIsNotFound() {
        Portfolio growth = createGrowth();

        assertThrows(PortfolioNotFoundException.class, () -> positionService.updatePositionAfterBuy(
            OTHER_ACCOUNT_ID, growth.getPortfolioId(), SYMBOL, 1, new BigDecimal("100.00"), null));
    }

    @Test
    @DisplayName("The database rejects a holding whose account doesn't match its portfolio")
    void databaseRejectsMismatchedAccount() {
        Portfolio growth = createGrowth();

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            "INSERT INTO positions (account_id, portfolio_id, symbol, quantity, average_cost) VALUES (?, ?, ?, 1, 1)",
            OTHER_ACCOUNT_ID, growth.getPortfolioId(), SYMBOL));
    }

    @Test
    @DisplayName("Main can't be deleted")
    void mainCannotBeDeleted() {
        Portfolio main = portfolioService.resolve(ACCOUNT_ID, null);

        assertThrows(PortfolioConflictException.class,
            () -> portfolioService.deletePortfolio(ACCOUNT_ID, main.getPortfolioId()));
        assertEquals(1, count("SELECT COUNT(*) FROM portfolios WHERE account_id = ?", ACCOUNT_ID));
    }

    @Test
    @DisplayName("Deleting a portfolio moves its holdings into Main, combining a symbol Main already holds")
    void deleteMovesHoldingsIntoMain() throws Exception {
        Portfolio growth = createGrowth();
        positionService.updatePositionAfterBuy(ACCOUNT_ID, null, SYMBOL, 10, new BigDecimal("100.00"), "ORD-10");
        positionService.updatePositionAfterBuy(ACCOUNT_ID, growth.getPortfolioId(), SYMBOL, 4,
            new BigDecimal("150.00"), "ORD-11");
        positionService.updatePositionAfterBuy(ACCOUNT_ID, growth.getPortfolioId(), "NVDA", 2,
            new BigDecimal("500.00"), "ORD-12");

        portfolioService.deletePortfolio(ACCOUNT_ID, growth.getPortfolioId());

        assertEquals(0, count("SELECT COUNT(*) FROM portfolios WHERE portfolio_id = ?", growth.getPortfolioId()));
        Position aapl = positionService.getPosition(ACCOUNT_ID, null, SYMBOL).orElseThrow();
        Position nvda = positionService.getPosition(ACCOUNT_ID, null, "NVDA").orElseThrow();
        assertDecimal("14", aapl.getQuantity());
        assertDecimal("114.2857", aapl.getAverageCost());    // (10 x 100 + 4 x 150) / 14
        assertDecimal("2", nvda.getQuantity());
        assertDecimal("500", nvda.getAverageCost());
        assertEquals(2, count("SELECT COUNT(*) FROM positions WHERE account_id = ?", ACCOUNT_ID));

        // The order history still records where each order originally went
        assertEquals(growth.getPortfolioId(), jdbcTemplate.queryForObject(
            "SELECT portfolio_id FROM position_movements WHERE order_id = 'ORD-11'", String.class));

        // And the combined holding behaves like any other: a sell from Main can use all 14
        positionService.updatePositionAfterSell(ACCOUNT_ID, null, SYMBOL, 14, null);
        assertEquals(1, count("SELECT COUNT(*) FROM positions WHERE account_id = ?", ACCOUNT_ID));
    }

    @Test
    @DisplayName("Deleting an empty portfolio just removes it")
    void deleteEmptyPortfolio() {
        Portfolio income = portfolioService.createPortfolio(ACCOUNT_ID,
            new CreatePortfolioRequest("Income", PortfolioRisk.LOW, null));

        portfolioService.deletePortfolio(ACCOUNT_ID, income.getPortfolioId());

        assertEquals(1, count("SELECT COUNT(*) FROM portfolios WHERE account_id = ?", ACCOUNT_ID));
    }

    @Test
    @DisplayName("Portfolios list Main first, then by name")
    void listOrder() {
        portfolioService.createPortfolio(ACCOUNT_ID, new CreatePortfolioRequest("Zebra", PortfolioRisk.LOW, null));
        portfolioService.createPortfolio(ACCOUNT_ID, new CreatePortfolioRequest("Alpha", PortfolioRisk.HIGH, null));

        List<String> names = portfolioService.getPortfolios(ACCOUNT_ID).stream().map(Portfolio::getName).toList();

        assertEquals(List.of("Main", "Alpha", "Zebra"), names);
    }

    private Portfolio createGrowth() {
        return portfolioService.createPortfolio(ACCOUNT_ID,
            new CreatePortfolioRequest("Growth", PortfolioRisk.HIGH, "Nasdaq-100"));
    }

    private int count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }
}
