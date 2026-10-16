package com.neueda.accountservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.neueda.accountservice.clients.CheckoutSession;
import com.neueda.accountservice.clients.PaymentGateway;
import com.neueda.accountservice.exceptions.AccountAuthorizationException;
import com.neueda.accountservice.services.TopUpService;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class TopUpConfirmIntegrationTest {

    private static final String ACCOUNT_ID = "ACC0001";
    private static final String SESSION_ID = "cs_test_a1B2c3";

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
    private TopUpService topUpService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private PaymentGateway paymentGateway;

    @BeforeEach
    void seedAccount() {
        jdbcTemplate.update("DELETE FROM cash_movements");
        jdbcTemplate.update("DELETE FROM accounts");
        jdbcTemplate.update(
                "INSERT INTO accounts (account_id, user_id, holder_name, cash_balance, status) "
                        + "VALUES (?, 1, 'Alice', 1000.00, 'ACTIVE')",
                ACCOUNT_ID);
        when(paymentGateway.isAvailable()).thenReturn(true);
    }

    private void stripeSays(String accountId) {
        when(paymentGateway.retrieveCheckout(SESSION_ID))
                .thenReturn(new CheckoutSession(SESSION_ID, null, "paid", "pi_3Abc", 25000L, "usd", accountId));
    }

    private BigDecimal balance() {
        return jdbcTemplate.queryForObject(
                "SELECT cash_balance FROM accounts WHERE account_id = ?", BigDecimal.class, ACCOUNT_ID);
    }

    private int movements() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cash_movements WHERE order_id = 'stripe:pi_3Abc' AND movement_type = 'CREDIT'",
                Integer.class);
    }

    @Test
    @DisplayName("Confirming the same payment twice adds the money once")
    void confirmTwiceCreditsOnce() throws Exception {
        stripeSays(ACCOUNT_ID);

        topUpService.confirmTopUp(ACCOUNT_ID, SESSION_ID);
        var second = topUpService.confirmTopUp(ACCOUNT_ID, SESSION_ID);

        assertThat(second.amount()).isEqualByComparingTo("250.00");
        assertThat(second.cashBalance()).isEqualByComparingTo("1250.00");
        assertThat(balance()).isEqualByComparingTo("1250.00");
        assertThat(movements()).isEqualTo(1);
    }

    @Test
    @DisplayName("A payment made for another account adds nothing")
    void otherAccountsPaymentAddsNothing() {
        stripeSays("ACC0099");

        assertThatThrownBy(() -> topUpService.confirmTopUp(ACCOUNT_ID, SESSION_ID))
                .isInstanceOf(AccountAuthorizationException.class);
        assertThat(balance()).isEqualByComparingTo("1000.00");
        assertThat(movements()).isZero();
    }
}
