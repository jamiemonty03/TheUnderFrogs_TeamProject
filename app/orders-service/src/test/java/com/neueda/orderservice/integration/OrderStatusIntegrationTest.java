package com.neueda.orderservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.neueda.orderservice.enums.OrderStatus;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OrderStatusIntegrationTest {

    private static final String ACCOUNT_ID = "ACC0001";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orders_status")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/orders-characterisation-schema.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void clearOrders() {
        jdbcTemplate.update("DELETE FROM orders");
    }

    @ParameterizedTest(name = "NEW -> {0} is allowed")
    @EnumSource(value = OrderStatus.class, names = {"FILLED", "REJECTED", "CANCELLED"})
    @DisplayName("Each allowed change from NEW returns 200 and updates the order once")
    void allowedChangeFromNewReturns200(OrderStatus newStatus) throws Exception {
        String orderId = insertOrder("NEW");

        ResponseEntity<String> response = patchStatus(orderId, "NEW", newStatus.name());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(response).get("orderStatus").asText()).isEqualTo(newStatus.name());
        Map<String, Object> row = row(orderId);
        assertThat(row.get("order_status")).isEqualTo(newStatus.name());
        assertThat(row.get("version")).isEqualTo(1);
    }

    @Test
    @DisplayName("Repeating the same change returns 409 with the current status and changes nothing")
    void repeatedChangeReturns409WithCurrentStatus() throws Exception {
        String orderId = insertOrder("NEW");
        patchStatus(orderId, "NEW", "FILLED");

        ResponseEntity<String> response = patchStatus(orderId, "NEW", "FILLED");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode body = json(response);
        assertThat(body.get("errorCode").asText()).isEqualTo("ORD-409");
        assertThat(body.get("currentStatus").asText()).isEqualTo("FILLED");
        assertThat(row(orderId).get("version")).isEqualTo(1);
    }

    @Test
    @DisplayName("An order no longer in expectedStatus returns 409 with its current status")
    void orderNotInExpectedStatusReturns409() throws Exception {
        String orderId = insertOrder("CANCELLED");

        ResponseEntity<String> response = patchStatus(orderId, "NEW", "FILLED");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(json(response).get("currentStatus").asText()).isEqualTo("CANCELLED");
        Map<String, Object> row = row(orderId);
        assertThat(row.get("order_status")).isEqualTo("CANCELLED");
        assertThat(row.get("version")).isEqualTo(0);
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({"FILLED,NEW", "CANCELLED,FILLED", "REJECTED,FILLED", "NEW,NEW", "FILLED,CANCELLED"})
    @DisplayName("A change that is not NEW -> FILLED/REJECTED/CANCELLED returns 422 and changes nothing")
    void disallowedChangeReturns422(String expectedStatus, String newStatus) throws Exception {
        String orderId = insertOrder(expectedStatus);

        ResponseEntity<String> response = patchStatus(orderId, expectedStatus, newStatus);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(response).get("errorCode").asText()).isEqualTo("VAL-422");
        Map<String, Object> row = row(orderId);
        assertThat(row.get("order_status")).isEqualTo(expectedStatus);
        assertThat(row.get("version")).isEqualTo(0);
    }

    @Test
    @DisplayName("An unknown order returns 404")
    void unknownOrderReturns404() throws Exception {
        ResponseEntity<String> response = patchStatus(UUID.randomUUID().toString(), "NEW", "FILLED");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(json(response).get("errorCode").asText()).isEqualTo("ORD-404");
    }

    @Test
    @DisplayName("A request without newStatus returns 422")
    void missingNewStatusReturns422() throws Exception {
        String orderId = insertOrder("NEW");

        ResponseEntity<String> response = send(orderId, "{\"expectedStatus\": \"NEW\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(row(orderId).get("order_status")).isEqualTo("NEW");
    }

    @Test
    @DisplayName("The trade executor's service token can change the status")
    void serviceTokenCanChangeStatus() throws Exception {
        String orderId = insertOrder("NEW");

        ResponseEntity<String> response = send(orderId,
                "{\"expectedStatus\": \"NEW\", \"newStatus\": \"FILLED\", \"reason\": \"filled by executor\"}",
                createToken("trade-executor", List.of("SERVICE")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(row(orderId).get("order_status")).isEqualTo("FILLED");
    }

    private String insertOrder(String status) {
        String orderId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO orders (order_id, idempotency_key, account_id, symbol, side, quantity, price, order_status)
                VALUES (?, ?, ?, 'AAPL', 'BUY', 10, 150.00, ?)
                """, orderId, "idem-" + orderId, ACCOUNT_ID, status);
        return orderId;
    }

    private Map<String, Object> row(String orderId) {
        return jdbcTemplate.queryForMap("SELECT order_status, version FROM orders WHERE order_id = ?", orderId);
    }

    private ResponseEntity<String> patchStatus(String orderId, String expectedStatus, String newStatus)
            throws Exception {
        return send(orderId, """
                {"expectedStatus": "%s", "newStatus": "%s", "reason": "test"}
                """.formatted(expectedStatus, newStatus));
    }

    private ResponseEntity<String> send(String orderId, String body) throws Exception {
        return send(orderId, body, createToken("status-test-user", null));
    }

    private ResponseEntity<String> send(String orderId, String body, String token) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(patch("/orders/{orderId}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse();
        return new ResponseEntity<>(response.getContentAsString(), HttpStatus.valueOf(response.getStatus()));
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private String createToken(String subject, List<String> roles) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(300)));
        if (roles != null) {
            claims.claim("roles", roles);
        }
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
        jwt.sign(new MACSigner(jwtSecret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
