package com.neueda.accountservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.neueda.accountservice.dtos.requests.CreateAccountRequest;
import com.neueda.accountservice.models.Account;
import com.neueda.accountservice.services.AccountService;

@SpringBootTest
@Testcontainers
class AccountCreationIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
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

    @BeforeEach
    void seedAccounts() {
        jdbcTemplate.update("DELETE FROM cash_movements");
        jdbcTemplate.update("DELETE FROM accounts");
        jdbcTemplate.execute("ALTER SEQUENCE account_number_seq RESTART WITH 1");
        for (int i = 1; i <= 3; i++) {
            jdbcTemplate.update(
                    "INSERT INTO accounts (account_id, user_id, holder_name, cash_balance, status) VALUES (?, ?, 'Seed', 500.00, 'ACTIVE')",
                    String.format("ACC%04d", i), i);
        }
    }

    @Test
    @DisplayName("A new account gets the next free ID from the sequence, skipping seeded accounts")
    void createsAccountWithNextFreeId() {
        Account created = accountService.createAccount(new CreateAccountRequest(12L, "Zed Smith"));

        assertEquals("ACC0004", created.getAccountId());
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT user_id, holder_name, cash_balance, status FROM accounts WHERE account_id = 'ACC0004'");
        assertEquals(12L, ((Number) row.get("user_id")).longValue());
        assertEquals("Zed Smith", row.get("holder_name"));
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) row.get("cash_balance")));
        assertEquals("ACTIVE", row.get("status"));
    }

    @Test
    @DisplayName("Seeded accounts are left untouched")
    void leavesSeededAccountsAlone() {
        accountService.createAccount(new CreateAccountRequest(12L, "Zed Smith"));

        assertEquals(0, new BigDecimal("500.00").compareTo(
                jdbcTemplate.queryForObject("SELECT cash_balance FROM accounts WHERE account_id = 'ACC0001'", BigDecimal.class)));
        assertEquals(4, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM accounts", Integer.class));
    }

    @Test
    @DisplayName("Each new account gets a different ID")
    void eachAccountGetsItsOwnId() {
        String first = accountService.createAccount(new CreateAccountRequest(12L, "Zed Smith")).getAccountId();
        String second = accountService.createAccount(new CreateAccountRequest(13L, "Amy Jones")).getAccountId();

        assertNotEquals(first, second);
        assertEquals("ACC0005", second);
    }
}
