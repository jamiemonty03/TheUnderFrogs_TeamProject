package com.neueda.orderservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.neueda.orderservice.repositories.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Records the pre-Kafka HTTP behavior of POST /orders through the real
 * controller, processor, strategy, service, and repository chain.
 */
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "service.accounts.url=http://accounts.test/api/accounts",
            "service.instruments.url=http://instruments.test/api/instruments"
        })
class OrderPlacementCharacterisationTest {

    private static final String ACCOUNTS_URL = "http://accounts.test/api/accounts";
    private static final String INSTRUMENTS_URL = "http://instruments.test/api/instruments";
    private static final String ACCOUNT_ID = "ACC0001";
    private static final String SYMBOL = "AAPL";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orders_characterisation")
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

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate http;

    private MockRestServiceServer downstream;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String configuredJwtSecret;

    @BeforeEach
    void clearOrders() {
        downstream.reset();
        jdbcTemplate.update("DELETE FROM orders");
    }

    @AfterEach
    void verifyDownstreamCalls() {
        downstream.verify();
    }

    @Autowired
    void bindDownstreamServer(RestTemplate restTemplate) {
        downstream = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void validBuyReturnsCreatedFilledOrderAndDebitsAccount() throws Exception {
        String token = createToken();
        expectAccountAndInstrumentLookups(token, "1000.00");
        downstream.expect(requestTo(ACCOUNTS_URL + "/" + ACCOUNT_ID + "/debit"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(content().json("{\"amount\":100.00}"))
                .andRespond(withSuccess(accountJson("900.00"), MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = postOrder(token, orderRequest("BUY", "buy-" + UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertCreatedOrderResponse(body, "BUY");
        assertThat(orderRepository.findAllByOrderByCreatedAtDesc()).hasSize(1);
    }

    @Test
    void validSellReturnsCreatedFilledOrderAndCreditsAccount() throws Exception {
        String token = createToken();
        expectAccountAndInstrumentLookups(token, "1000.00");
        downstream.expect(requestTo(ACCOUNTS_URL + "/" + ACCOUNT_ID + "/credit"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(content().json("{\"amount\":100.00}"))
                .andRespond(withSuccess(accountJson("1100.00"), MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = postOrder(token, orderRequest("SELL", "sell-" + UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertCreatedOrderResponse(body, "SELL");
        assertThat(orderRepository.findAllByOrderByCreatedAtDesc()).hasSize(1);
    }

    @Test
    void insufficientFundsReturnsBadRequestOrd400() throws Exception {
        String token = createToken();
        expectAccountAndInstrumentLookups(token, "50.00");

        ResponseEntity<String> response = postOrder(token, orderRequest("BUY", "funds-" + UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(objectMapper.readTree(response.getBody()).path("errorCode").asText()).isEqualTo("ORD-400");
        assertThat(orderRepository.findAllByOrderByCreatedAtDesc()).isEmpty();
    }

    @Test
    void invalidRequestBodyReturnsUnprocessableEntityVal422() throws Exception {
        ResponseEntity<String> response = postOrder(createToken(), """
                {"accountId":"ACC0001","symbol":"AAPL","side":"BUY","quantity":0,"price":50,"idempotencyKey":"invalid"}
                """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(objectMapper.readTree(response.getBody()).path("errorCode").asText()).isEqualTo("VAL-422");
    }

    @Test
    void unknownAccountReturnsNotFoundAcc404() throws Exception {
        String token = createToken();
        downstream.expect(requestTo(ACCOUNTS_URL + "/" + ACCOUNT_ID))
                .andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ResponseEntity<String> response = postOrder(token, orderRequest("BUY", "account-" + UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(objectMapper.readTree(response.getBody()).path("errorCode").asText()).isEqualTo("ACC-404");
    }

    @Test
    void missingInstrumentReturnsNotFoundIns404() throws Exception {
        String token = createToken();
        expectAccountLookup(token, "1000.00");
        downstream.expect(requestTo(INSTRUMENTS_URL + "/" + SYMBOL))
                .andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ResponseEntity<String> response = postOrder(token, orderRequest("BUY", "instrument-" + UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(objectMapper.readTree(response.getBody()).path("errorCode").asText()).isEqualTo("INS-404");
    }

    @Test
    void failedBuyExecutionReturnsUnprocessableEntityOrd422() throws Exception {
        String token = createToken();
        expectAccountAndInstrumentLookups(token, "1000.00");
        downstream.expect(requestTo(ACCOUNTS_URL + "/" + ACCOUNT_ID + "/debit"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(content().json("{\"amount\":100.00}"))
                .andRespond(withServerError());

        ResponseEntity<String> response = postOrder(token, orderRequest("BUY", "failed-" + UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(objectMapper.readTree(response.getBody()).path("errorCode").asText()).isEqualTo("ORD-422");
        assertThat(orderRepository.findAllByOrderByCreatedAtDesc()).hasSize(1);
        assertThat(orderRepository.findAllByOrderByCreatedAtDesc().get(0).getOrderStatus()).isEqualTo(com.neueda.orderservice.enums.OrderStatus.REJECTED);
    }

    @Test
    void duplicateIdempotencyKeyReturnsConflictOrd409() throws Exception {
        String token = createToken();
        String idempotencyKey = "duplicate-" + UUID.randomUUID();
        expectAccountAndInstrumentLookups(token, "1000.00");
        downstream.expect(requestTo(ACCOUNTS_URL + "/" + ACCOUNT_ID + "/debit"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(content().json("{\"amount\":100.00}"))
                .andRespond(withSuccess(accountJson("900.00"), MediaType.APPLICATION_JSON));
        expectAccountAndInstrumentLookups(token, "1000.00");

        ResponseEntity<String> first = postOrder(token, orderRequest("BUY", idempotencyKey));
        ResponseEntity<String> duplicate = postOrder(token, orderRequest("BUY", idempotencyKey));

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(objectMapper.readTree(duplicate.getBody()).path("errorCode").asText()).isEqualTo("ORD-409");
        assertThat(orderRepository.findAllByOrderByCreatedAtDesc()).hasSize(1);
    }

    @Test
    void missingBearerTokenReturnsUnauthorized() {
        ResponseEntity<String> response = http.postForEntity(
                ordersUrl(),
                new HttpEntity<>(orderRequest("BUY", "no-token-" + UUID.randomUUID()), jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidBearerTokenReturnsUnauthorized() {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth("not-a-valid-jwt");

        ResponseEntity<String> response = http.postForEntity(
                ordersUrl(),
                new HttpEntity<>(orderRequest("BUY", "bad-token-" + UUID.randomUUID()), headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void assertCreatedOrderResponse(JsonNode body, String side) {
        assertThat(body.path("orderId").asText()).isNotBlank();
        assertThat(body.path("accountId").asText()).isEqualTo(ACCOUNT_ID);
        assertThat(body.path("symbol").asText()).isEqualTo(SYMBOL);
        assertThat(body.path("side").asText()).isEqualTo(side);
        assertThat(body.path("quantity").asInt()).isEqualTo(2);
        assertThat(body.path("price").decimalValue()).isEqualByComparingTo("50.00");
        // S7-3 intentionally changes this baseline business status from FILLED to NEW.
        assertThat(body.path("orderStatus").asText()).isEqualTo("FILLED");
        assertThat(body.path("version").asInt()).isZero();
        assertThat(body.path("createdAt").asText()).isNotBlank();
        assertThat(body.path("lastUpdated").asText()).isNotBlank();
        assertThat(body.path("updatedBy").asText()).isEqualTo("SYSTEM");
    }

    private void expectAccountAndInstrumentLookups(String token, String cashBalance) {
        expectAccountLookup(token, cashBalance);
        downstream.expect(requestTo(INSTRUMENTS_URL + "/" + SYMBOL))
                .andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andRespond(withSuccess(instrumentJson(), MediaType.APPLICATION_JSON));
    }

    private void expectAccountLookup(String token, String cashBalance) {
        downstream.expect(requestTo(ACCOUNTS_URL + "/" + ACCOUNT_ID))
                .andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andRespond(withSuccess(accountJson(cashBalance), MediaType.APPLICATION_JSON));
    }

    private ResponseEntity<String> postOrder(String token, String body) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return http.postForEntity(ordersUrl(), new HttpEntity<>(body, headers), String.class);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String ordersUrl() {
        return "http://localhost:" + port + "/orders";
    }

    private String orderRequest(String side, String idempotencyKey) {
        return """
                {"accountId":"ACC0001","symbol":"AAPL","side":"%s","quantity":2,"price":50.00,"idempotencyKey":"%s"}
                """.formatted(side, idempotencyKey);
    }

    private String accountJson(String cashBalance) {
        return """
                {"accountId":"ACC0001","holderName":"Alice Johnson","cashBalance":%s,"status":"ACTIVE"}
                """.formatted(cashBalance);
    }

    private String instrumentJson() {
        return """
                {"symbol":"AAPL","name":"Apple Inc.","price":50.00,"tradable":true}
                """;
    }

    private String createToken() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("characterisation-user")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(configuredJwtSecret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
