package com.neueda.orderservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.orderservice.config.TestJwtIssuer;
import com.neueda.orderservice.events.EventEnvelope;
import com.neueda.orderservice.events.OrderPlacedPayload;
import com.neueda.orderservice.events.Topics;

/** POST /orders with an optional portfolioId, through the real controller, processor and database. */
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "service.accounts.url=http://accounts.test/api/accounts",
            "service.instruments.url=http://instruments.test/api/instruments",
            "service.portfolios.url=http://portfolios.test/api/portfolios"
        })
class OrderPortfolioIntegrationTest {

    private static final String ACCOUNT_ID = "ACC0001";
    private static final String SYMBOL = "AAPL";
    private static final String PORTFOLIO_ID = "PF0012";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orders_portfolios")
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
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private MockRestServiceServer downstream;

    @Autowired
    void bindDownstreamServer(RestTemplate restTemplate) {
        downstream = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @BeforeEach
    void clearOrders() {
        downstream.reset();
        reset(kafkaTemplate);
        jdbcTemplate.update("DELETE FROM orders");
    }

    @AfterEach
    void verifyDownstreamCalls() {
        downstream.verify();
    }

    @Test
    @DisplayName("An order for a portfolio checks it with positions-service, stores it and publishes it")
    void orderForPortfolioIsStoredAndPublished() throws Exception {
        String token = token();
        expectAccountAndInstrumentLookups(token);
        downstream.expect(requestTo("http://portfolios.test/api/portfolios/" + ACCOUNT_ID + "/" + PORTFOLIO_ID))
                .andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andRespond(withSuccess("{\"portfolioId\":\"PF0012\"}", MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = postOrder(token, orderRequest(PORTFOLIO_ID));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("portfolioId").asText()).isEqualTo(PORTFOLIO_ID);
        assertThat(jdbcTemplate.queryForObject("SELECT portfolio_id FROM orders", String.class))
                .isEqualTo(PORTFOLIO_ID);
        assertThat(publishedPayload().portfolioId()).isEqualTo(PORTFOLIO_ID);
    }

    @Test
    @DisplayName("Without a portfolioId there is no portfolio lookup, and the order is for Main (null)")
    void orderWithoutPortfolioIsForMain() throws Exception {
        String token = token();
        expectAccountAndInstrumentLookups(token);

        ResponseEntity<String> response = postOrder(token, orderRequest(null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(objectMapper.readTree(response.getBody()).path("portfolioId").isNull()).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT portfolio_id FROM orders", String.class)).isNull();
        assertThat(publishedPayload().portfolioId()).isNull();
    }

    @Test
    @DisplayName("A portfolio the account doesn't have is 404 PFL-404, and no order is saved")
    void unknownPortfolioIsRejected() throws Exception {
        String token = token();
        expectAccountAndInstrumentLookups(token);
        downstream.expect(requestTo("http://portfolios.test/api/portfolios/" + ACCOUNT_ID + "/PF9999"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ResponseEntity<String> response = postOrder(token, orderRequest("PF9999"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(objectMapper.readTree(response.getBody()).path("errorCode").asText()).isEqualTo("PFL-404");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("If positions-service can't be reached the order is refused with 503 PFL-503")
    void portfolioServiceDownIsServiceUnavailable() throws Exception {
        String token = token();
        expectAccountAndInstrumentLookups(token);
        downstream.expect(requestTo("http://portfolios.test/api/portfolios/" + ACCOUNT_ID + "/" + PORTFOLIO_ID))
                .andRespond(withServerError());

        ResponseEntity<String> response = postOrder(token, orderRequest(PORTFOLIO_ID));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(objectMapper.readTree(response.getBody()).path("errorCode").asText()).isEqualTo("PFL-503");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
        verifyNoInteractions(kafkaTemplate);
    }

    private OrderPlacedPayload publishedPayload() {
        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(kafkaTemplate).send(eq(Topics.ORDERS), eq(ACCOUNT_ID), captor.capture());
        return (OrderPlacedPayload) captor.getValue().payload();
    }

    private void expectAccountAndInstrumentLookups(String token) {
        downstream.expect(requestTo("http://accounts.test/api/accounts/" + ACCOUNT_ID))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andRespond(withSuccess("""
                        {"accountId":"ACC0001","holderName":"Alice Johnson","cashBalance":1000.00,"status":"ACTIVE"}
                        """, MediaType.APPLICATION_JSON));
        downstream.expect(requestTo("http://instruments.test/api/instruments/" + SYMBOL))
                .andRespond(withSuccess("""
                        {"symbol":"AAPL","name":"Apple Inc.","price":50.00,"tradable":true}
                        """, MediaType.APPLICATION_JSON));
    }

    private ResponseEntity<String> postOrder(String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return http.postForEntity("http://localhost:" + port + "/orders", new HttpEntity<>(body, headers), String.class);
    }

    private static String orderRequest(String portfolioId) {
        String portfolio = portfolioId == null ? "" : ",\"portfolioId\":\"" + portfolioId + "\"";
        return """
                {"accountId":"ACC0001","symbol":"AAPL","side":"BUY","quantity":2,"priceLimit":50.00,"idempotencyKey":"%s"%s}
                """.formatted("pf-" + UUID.randomUUID(), portfolio);
    }

    private static String token() {
        return TestJwtIssuer.token().subject("portfolio-user").accountId(ACCOUNT_ID).build();
    }
}
