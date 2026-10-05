package com.neueda.tradeexecutor.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.tradeexecutor.clients.AccountsClient;
import com.neueda.tradeexecutor.clients.AlpacaPriceSource;
import com.neueda.tradeexecutor.clients.InstrumentsClient;
import com.neueda.tradeexecutor.clients.OrdersClient;
import com.neueda.tradeexecutor.clients.PositionsClient;
import com.neueda.tradeexecutor.dtos.InstrumentDto;
import com.neueda.tradeexecutor.dtos.OrderDto;
import com.neueda.tradeexecutor.dtos.StatusUpdateResult;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.enums.OrderStatus;
import com.neueda.tradeexecutor.events.EventTypes;
import com.neueda.tradeexecutor.events.Topics;

@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=duplicate-replay-test-secret-32-bytes",
        "spring.kafka.consumer.group-id=trade-executor-duplicate-test"
})
class DuplicateOrderPlacedIntegrationTest {

    private static final String ACCOUNT_ID = "ACC0001";
    private static final String SYMBOL = "AAPL";
    private static final BigDecimal LIMIT = new BigDecimal("150.00");
    private static final BigDecimal MARKET = new BigDecimal("145.00");
    private static final BigDecimal DEBIT = new BigDecimal("1450.00");
    private static final Duration WAIT = Duration.ofSeconds(20);

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.1");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @BeforeAll
    static void createTopics() throws Exception {
        try (AdminClient admin = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(
                    new NewTopic(Topics.ORDERS, 3, (short) 1),
                    new NewTopic(Topics.TRADE_EVENTS, 3, (short) 1),
                    new NewTopic(Topics.ORDERS_DLT, 3, (short) 1))).all().get();
        }
    }

    @MockBean
    private OrdersClient ordersClient;

    @MockBean
    private AccountsClient accountsClient;

    @MockBean
    private PositionsClient positionsClient;

    @MockBean
    private InstrumentsClient instrumentsClient;

    @MockBean
    private AlpacaPriceSource priceSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void stubInstrumentAndPrice() {
        InstrumentDto instrument = new InstrumentDto(SYMBOL, "STOCK", true);
        when(instrumentsClient.getInstrument(SYMBOL)).thenReturn(Optional.of(instrument));
        when(priceSource.getPrice(any(), any())).thenReturn(Optional.of(MARKET));
    }

    @Test
    @DisplayName("Replaying ORDER_PLACED after the order is FILLED is skipped: one debit, one position, one trade event")
    void replayAfterFillIsSkipped() throws Exception {
        String orderId = "ORD-A-" + UUID.randomUUID();
        when(ordersClient.getOrder(orderId)).thenReturn(order(orderId, OrderStatus.NEW), order(orderId, OrderStatus.FILLED));
        when(ordersClient.updateStatus(eq(orderId), eq(OrderStatus.FILLED), any()))
                .thenReturn(new StatusUpdateResult(true, OrderStatus.FILLED));
        String message = orderPlacedMessage(orderId);

        publish(message);
        verify(ordersClient, timeout(WAIT.toMillis())).updateStatus(eq(orderId), eq(OrderStatus.FILLED), any());

        publish(message);
        verify(ordersClient, timeout(WAIT.toMillis()).times(2)).getOrder(orderId);

        verify(accountsClient, times(1)).debit(ACCOUNT_ID, orderId, DEBIT);
        verify(positionsClient, times(1)).addPosition(ACCOUNT_ID, SYMBOL, orderId, 10, MARKET);
        verify(ordersClient, times(1)).updateStatus(anyString(), any(), any());
        assertThat(tradeEventsFor(orderId)).singleElement()
                .satisfies(event -> assertThat(event.get("eventType").asText()).isEqualTo(EventTypes.ORDER_FILLED));
    }

    @Test
    @DisplayName("A replay that arrives before the status update repeats the same idempotent calls and publishes no second event")
    void replayBeforeStatusUpdatePublishesOnce() throws Exception {
        String orderId = "ORD-B-" + UUID.randomUUID();
        when(ordersClient.getOrder(orderId)).thenReturn(order(orderId, OrderStatus.NEW));
        when(ordersClient.updateStatus(eq(orderId), eq(OrderStatus.FILLED), any()))
                .thenReturn(new StatusUpdateResult(true, OrderStatus.FILLED))
                .thenReturn(new StatusUpdateResult(false, OrderStatus.FILLED));
        String message = orderPlacedMessage(orderId);

        publish(message);
        verify(ordersClient, timeout(WAIT.toMillis())).updateStatus(eq(orderId), eq(OrderStatus.FILLED), any());

        publish(message);
        verify(ordersClient, timeout(WAIT.toMillis()).times(2)).updateStatus(eq(orderId), eq(OrderStatus.FILLED), any());

        verify(accountsClient, times(2)).debit(ACCOUNT_ID, orderId, DEBIT);
        verify(positionsClient, times(2)).addPosition(ACCOUNT_ID, SYMBOL, orderId, 10, MARKET);
        verify(accountsClient, never()).reverse(anyString(), anyString());
        assertThat(tradeEventsFor(orderId)).hasSize(1);
    }

    private OrderDto order(String orderId, OrderStatus status) {
        return new OrderDto(orderId, ACCOUNT_ID, SYMBOL, OrderSide.BUY, 10, LIMIT, status);
    }

    private String orderPlacedMessage(String orderId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", EventTypes.ORDER_PLACED,
                "occurredAt", Instant.now().toString(),
                "key", ACCOUNT_ID,
                "payload", Map.of(
                        "orderId", orderId,
                        "accountId", ACCOUNT_ID,
                        "symbol", SYMBOL,
                        "side", "BUY",
                        "quantity", 10,
                        "price", LIMIT,
                        "idempotencyKey", "key-" + orderId)));
    }

    private void publish(String message) throws Exception {
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            producer.send(new ProducerRecord<>(Topics.ORDERS, ACCOUNT_ID, message)).get();
        }
    }

    private List<JsonNode> tradeEventsFor(String orderId) throws Exception {
        List<JsonNode> events = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "trade-events-check-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumer.subscribe(List.of(Topics.TRADE_EVENTS));
            long deadline = System.currentTimeMillis() + Duration.ofSeconds(8).toMillis();
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    JsonNode event = objectMapper.readTree(record.value());
                    if (orderId.equals(event.path("payload").path("orderId").asText())) {
                        events.add(event);
                    }
                }
            }
        }
        return events;
    }
}
