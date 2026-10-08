package com.neueda.orderservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.neueda.orderservice.config.TestJwtIssuer;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.events.EventEnvelope;
import com.neueda.orderservice.events.EventTypes;
import com.neueda.orderservice.events.OrderCancelledPayload;
import com.neueda.orderservice.events.Topics;
import com.neueda.orderservice.services.OrderCancellationService;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderCancellationIntegrationTest {

    private static final String ACCOUNT_ID = "ACC0001";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orders_cancellation")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/orders-characterisation-schema.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        TestJwtIssuer.registerProperties(registry);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderCancellationService cancellationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;


    @BeforeEach
    void clearOrders() {
        reset(kafkaTemplate);
        jdbcTemplate.update("DELETE FROM orders");
    }

    @Test
    void cancellingNewOrderReturns204SetsCancelledAndPublishesOneOrderCancelled() throws Exception {
        String orderId = insertOrder("NEW");

        ResponseEntity<String> response = cancel(orderId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Map<String, Object> row = row(orderId);
        assertThat(row.get("order_status")).isEqualTo("CANCELLED");
        assertThat(row.get("version")).isEqualTo(1);

        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate, times(1)).send(eq(Topics.TRADE_EVENTS), eq(ACCOUNT_ID), sent.capture());
        EventEnvelope<?> envelope = (EventEnvelope<?>) sent.getValue();
        assertThat(envelope.eventType()).isEqualTo(EventTypes.ORDER_CANCELLED);
        assertThat(envelope.key()).isEqualTo(ACCOUNT_ID);
        OrderCancelledPayload payload = (OrderCancelledPayload) envelope.payload();
        assertThat(payload.orderId()).isEqualTo(orderId);
        assertThat(payload.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FILLED", "REJECTED", "CANCELLED"})
    void cancellingOrderThatIsNoLongerNewReturns409AndChangesNothing(String status) throws Exception {
        String orderId = insertOrder(status);
        Map<String, Object> before = row(orderId);

        ResponseEntity<String> response = cancel(orderId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("errorCode").asText()).isEqualTo("ORD-409");
        assertThat(body.path("message").asText()).contains("status is " + status);
        assertThat(row(orderId)).isEqualTo(before);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void cancellingUnknownOrderReturns404AndPublishesNothing() throws Exception {
        ResponseEntity<String> response = cancel(UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(objectMapper.readTree(response.getBody()).path("errorCode").asText()).isEqualTo("ORD-404");
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void secondCancelOfSameOrderReturns409AndOnlyOneEventIsPublished() throws Exception {
        String orderId = insertOrder("NEW");

        assertThat(cancel(orderId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(cancel(orderId).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
    }

    @Test
    void cancelInATransactionThatRollsBackPublishesNothingAndLeavesOrderNew() {
        String orderId = insertOrder("NEW");

        transactionTemplate.executeWithoutResult(tx -> {
            try {
                cancellationService.cancel(orderId);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            tx.setRollbackOnly();
        });

        assertThat(row(orderId).get("order_status")).isEqualTo("NEW");
        verifyNoInteractions(kafkaTemplate);
    }

    private String insertOrder(String status) {
        String orderId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO orders (order_id, idempotency_key, account_id, symbol, side, quantity, price_limit, order_status)
                VALUES (?, ?, ?, 'AAPL', 'BUY', 10, 150.00, ?)
                """, orderId, "idem-" + orderId, ACCOUNT_ID, status);
        return orderId;
    }

    private Map<String, Object> row(String orderId) {
        return jdbcTemplate.queryForMap(
                "SELECT order_status, version, last_updated, updated_by FROM orders WHERE order_id = ?", orderId);
    }

    private ResponseEntity<String> cancel(String orderId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(createToken());
        return http.exchange("http://localhost:" + port + "/orders/" + orderId,
                HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
    }

    private String createToken() {
        return TestJwtIssuer.token().subject("cancellation-test-user").build();
    }
}
