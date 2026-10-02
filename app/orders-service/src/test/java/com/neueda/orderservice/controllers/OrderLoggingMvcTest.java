package com.neueda.orderservice.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.orderservice.config.CorrelationIdFilter;
import com.neueda.orderservice.config.SecurityConfig;
import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.exceptions.GlobalExceptionHandler;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.repositories.OrderRepository;
import com.neueda.orderservice.services.OrderCancellationService;
import com.neueda.orderservice.services.OrderStatusService;
import com.neueda.orderservice.services.orderServices.OrderProcessor;
import com.neueda.orderservice.services.orderServices.OrderResult;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import net.logstash.logback.encoder.LogstashEncoder;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderLoggingMvcTest {

    private static final String ORDER_BODY = """
            {"accountId": "ACC0001", "symbol": "AAPL", "side": "BUY",
             "quantity": 10, "price": 150.00, "idempotencyKey": "key-1"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderProcessor orderProcessor;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private OrderCancellationService orderCancellationService;

    @MockBean
    private OrderStatusService orderStatusService;

    private final CapturingAppender appender = new CapturingAppender();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void attachAppender() {
        appender.start();
        logger(OrderController.class).addAppender(appender);
        logger(GlobalExceptionHandler.class).addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger(OrderController.class).detachAppender(appender);
        logger(GlobalExceptionHandler.class).detachAppender(appender);
    }

    @Test
    @DisplayName("Every log line for a placed order carries the request's correlation id")
    void placedOrderLogsShareCorrelationId() throws Exception {
        stubSuccessfulPlacement();

        mockMvc.perform(post("/orders").with(jwt())
                        .header(CorrelationIdFilter.HEADER, "review-demo-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string(CorrelationIdFilter.HEADER, "review-demo-123"));

        ILoggingEvent received = event("Order placement received");
        ILoggingEvent placed = event("Order placed");
        assertThat(received.getMDCPropertyMap()).containsEntry(CorrelationIdFilter.MDC_KEY, "review-demo-123");
        assertThat(placed.getMDCPropertyMap()).containsEntry(CorrelationIdFilter.MDC_KEY, "review-demo-123");
    }

    @Test
    @DisplayName("The placed-order JSON log line has correlationId and orderId as separate fields")
    void placedOrderJsonLogHasStructuredFields() throws Exception {
        stubSuccessfulPlacement();

        mockMvc.perform(post("/orders").with(jwt())
                        .header(CorrelationIdFilter.HEADER, "review-demo-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_BODY))
                .andExpect(status().isCreated());

        JsonNode json = toJson(event("Order placed"));
        assertThat(json.get("correlationId").asText()).isEqualTo("review-demo-123");
        assertThat(json.get("orderId").asText()).isEqualTo("order-1");
        assertThat(json.get("accountId").asText()).isEqualTo("ACC0001");
        assertThat(json.get("symbol").asText()).isEqualTo("AAPL");
        assertThat(json.get("level").asText()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("Without a header, the generated correlation id is returned and used in the logs")
    void generatedCorrelationIdIsReturnedAndLogged() throws Exception {
        stubSuccessfulPlacement();

        MvcResult result = mockMvc.perform(post("/orders").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_BODY))
                .andExpect(status().isCreated())
                .andReturn();

        String generated = result.getResponse().getHeader(CorrelationIdFilter.HEADER);
        assertThat(generated).matches("[0-9a-f-]{36}");
        assertThat(event("Order placed").getMDCPropertyMap()).containsEntry(CorrelationIdFilter.MDC_KEY, generated);
    }

    @Test
    @DisplayName("A rejected request is logged with its correlation id and error code")
    void rejectedRequestIsLoggedWithCorrelationId() throws Exception {
        mockMvc.perform(post("/orders").with(jwt())
                        .header(CorrelationIdFilter.HEADER, "bad-request-456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\": \"ACC0001\","))
                .andExpect(status().isUnprocessableEntity());

        ILoggingEvent rejected = event("Request rejected");
        JsonNode json = toJson(rejected);
        assertThat(rejected.getLevel().toString()).isEqualTo("WARN");
        assertThat(json.get("correlationId").asText()).isEqualTo("bad-request-456");
        assertThat(json.get("errorCode").asText()).isEqualTo("VAL-422");
        assertThat(json.get("status").asInt()).isEqualTo(422);
    }

    @Test
    @DisplayName("Log lines never contain the bearer token")
    void logsDoNotContainToken() throws Exception {
        stubSuccessfulPlacement();

        mockMvc.perform(post("/orders").with(jwt())
                        .header("Authorization", "Bearer secret-token-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_BODY));

        assertThat(appender.events).allSatisfy(e ->
                assertThat(new String(encoder().encode(e), StandardCharsets.UTF_8)).doesNotContain("secret-token-value"));
    }

    private void stubSuccessfulPlacement() throws Exception {
        Account account = new Account("ACC0001", "Alice Johnson", new BigDecimal("10000.00"), AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple Inc.", new BigDecimal("150.00"), true);
        Order order = new Order("order-1", "ACC0001", "AAPL", OrderSide.BUY, 10, new BigDecimal("150.00"), "key-1");
        when(restTemplate.getForObject(anyString(), eq(Account.class), eq("ACC0001"))).thenReturn(account);
        when(restTemplate.getForObject(anyString(), eq(Instrument.class), eq("AAPL"))).thenReturn(instrument);
        when(orderProcessor.processOrder(any(), any(), any(), any(), any(), any()))
                .thenReturn(new OrderResult(true, "Order accepted", null, order));
    }

    private ILoggingEvent event(String messagePrefix) {
        return appender.events.stream()
                .filter(e -> e.getMessage().startsWith(messagePrefix))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No log line starting with: " + messagePrefix));
    }

    private JsonNode toJson(ILoggingEvent event) throws Exception {
        return objectMapper.readTree(encoder().encode(event));
    }

    private static LogstashEncoder encoder() {
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        encoder.start();
        return encoder;
    }

    private static Logger logger(Class<?> type) {
        return (Logger) LoggerFactory.getLogger(type);
    }

    private static final class CapturingAppender extends AppenderBase<ILoggingEvent> {

        private final List<ILoggingEvent> events = new CopyOnWriteArrayList<>();

        @Override
        protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            events.add(event);
        }
    }
}
