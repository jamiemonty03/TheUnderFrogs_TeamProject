package com.neueda.accountservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
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

import com.neueda.accountservice.exceptions.InsufficientFundsException;
import com.neueda.accountservice.models.Account;
import com.neueda.accountservice.repositories.AccountRepository;
import com.neueda.accountservice.services.AccountService;

@Testcontainers
@SpringBootTest
class IdempotentCashMovementIntegrationTest {

    private static final String ACCOUNT_ID = "ACC0001";

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
    private AccountService accountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private AccountRepository accountRepository;

    @BeforeEach
    void seedAccount() {
        jdbcTemplate.update("DELETE FROM cash_movements");
        jdbcTemplate.update("DELETE FROM accounts");
        jdbcTemplate.update("DELETE FROM users");
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (username, email, password) VALUES ('alice', 'alice@test.com', 'x') RETURNING id",
                Long.class);
        jdbcTemplate.update(
                "INSERT INTO accounts (account_id, user_id, holder_name, cash_balance, status) "
                        + "VALUES (?, ?, 'Alice', 1000.00, 'ACTIVE')",
                ACCOUNT_ID, userId);
    }

    @Test
    @DisplayName("Debit twice with the same orderId changes the balance once")
    void debitIsAppliedOncePerOrder() throws Exception {
        accountService.debit(ACCOUNT_ID, new BigDecimal("100.00"), "ORD-1");
        Account second = accountService.debit(ACCOUNT_ID, new BigDecimal("100.00"), "ORD-1");

        assertBalance("900.00", second.getCashBalance());
        assertBalance("900.00", balance());
        assertEquals(1, movements("ORD-1"));
    }

    @Test
    @DisplayName("Credit twice with the same orderId changes the balance once")
    void creditIsAppliedOncePerOrder() throws Exception {
        accountService.credit(ACCOUNT_ID, new BigDecimal("250.00"), "ORD-1");
        Account second = accountService.credit(ACCOUNT_ID, new BigDecimal("250.00"), "ORD-1");

        assertBalance("1250.00", second.getCashBalance());
        assertBalance("1250.00", balance());
        assertEquals(1, movements("ORD-1"));
    }

    @Test
    @DisplayName("Without an orderId every call is applied, as before")
    void debitWithoutOrderIdKeepsOldBehaviour() throws Exception {
        accountService.debit(ACCOUNT_ID, new BigDecimal("100.00"));
        accountService.debit(ACCOUNT_ID, new BigDecimal("100.00"));

        assertBalance("800.00", balance());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cash_movements", Integer.class));
    }

    @Test
    @DisplayName("Reversal undoes the original debit once, however many times it is called")
    void reversalIsAppliedOnce() throws Exception {
        accountService.debit(ACCOUNT_ID, new BigDecimal("100.00"), "ORD-2");

        accountService.reverse(ACCOUNT_ID, "ORD-2");
        Account second = accountService.reverse(ACCOUNT_ID, "ORD-2");

        assertBalance("1000.00", second.getCashBalance());
        assertBalance("1000.00", balance());
        assertEquals(2, movements("ORD-2"));
    }

    @Test
    @DisplayName("Reversing an order with no cash movement changes nothing")
    void reversalWithoutOriginalChangesNothing() throws Exception {
        accountService.reverse(ACCOUNT_ID, "ORD-NEVER-DEBITED");

        assertBalance("1000.00", balance());
        assertEquals(0, movements("ORD-NEVER-DEBITED"));
    }

    @Test
    @DisplayName("A failed check after the ledger insert rolls the ledger row back, so a retry still works")
    void insufficientFundsRollsBackLedgerRow() throws Exception {
        assertThrows(InsufficientFundsException.class,
                () -> accountService.debit(ACCOUNT_ID, new BigDecimal("2000.00"), "ORD-3"));
        assertEquals(0, movements("ORD-3"));

        accountService.credit(ACCOUNT_ID, new BigDecimal("5000.00"));
        accountService.debit(ACCOUNT_ID, new BigDecimal("2000.00"), "ORD-3");

        assertBalance("4000.00", balance());
        assertEquals(1, movements("ORD-3"));
    }

    @Test
    @DisplayName("A forced failure after the ledger insert rolls back both the ledger row and the balance")
    void forcedFailureRollsBackBothWrites() {
        doThrow(new RuntimeException("forced failure")).when(accountRepository).save(any(Account.class));

        assertThrows(RuntimeException.class,
                () -> accountService.debit(ACCOUNT_ID, new BigDecimal("100.00"), "ORD-4"));

        assertBalance("1000.00", balance());
        assertEquals(0, movements("ORD-4"));
    }

    private BigDecimal balance() {
        return jdbcTemplate.queryForObject(
                "SELECT cash_balance FROM accounts WHERE account_id = ?", BigDecimal.class, ACCOUNT_ID);
    }

    private int movements(String orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cash_movements WHERE order_id = ?", Integer.class, orderId);
    }

    private static void assertBalance(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected balance " + expected + " but was " + actual);
    }
}
