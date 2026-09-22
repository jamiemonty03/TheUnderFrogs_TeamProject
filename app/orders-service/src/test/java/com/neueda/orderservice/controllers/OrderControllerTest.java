package com.neueda.orderservice.controllers;

import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.repositories.InMemoryOrderRepository;
import com.neueda.orderservice.repositories.InMemoryPositionRepository;
import com.neueda.orderservice.repositories.OrderRepository;
import com.neueda.orderservice.repositories.PositionRepository;
import com.neueda.orderservice.services.OrderService;
import com.neueda.orderservice.dtos.requests.UpdateOrderRequest;
import com.neueda.orderservice.dtos.responses.OrderResponse;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderController Unit Tests
 * 
 * Tests are written to work with InMemoryOrderRepository for fast unit testing.
 * See Docker Integration Test section below for conversion to real PostgreSQL database.
 */
@DisplayName("OrderController Unit Tests")
public class OrderControllerTest {

    private OrderController orderController;
    private OrderRepository orderRepository;
    private OrderService orderService;
    private PositionRepository positionRepository;
    
    private Order testOrder1;
    private Order testOrder2;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
        positionRepository = new InMemoryPositionRepository();
        orderService = new OrderService(positionRepository);
        orderController = new OrderController(orderService, orderRepository);

        testOrder1 = new Order(
            "ORD001",
            "ACC001",
            "AAPL",
            OrderSide.BUY,
            100,
            new BigDecimal("150.25"),
            "idempotent-key-1"
        );

        testOrder2 = new Order(
            "ORD002",
            "ACC001",
            "MSFT",
            OrderSide.SELL,
            50,
            new BigDecimal("300.00"),
            "idempotent-key-2"
        );

        orderRepository.save(testOrder1);
        orderRepository.save(testOrder2);
    }

    @Test
    @DisplayName("GET /orders/{orderId} returns order when found")
    void testGetOrderByIdSuccess() {
        var response = orderController.getOrderById("ORD001");
        
        assertTrue(response.getStatusCode().is2xxSuccessful());
        OrderResponse order = response.getBody();
        assertEquals("ORD001", order.orderId());
        assertEquals("AAPL", order.symbol());
        assertEquals(OrderSide.BUY, order.side());
        assertEquals(100, order.quantity());
        assertEquals(new BigDecimal("150.25"), order.price());
        assertEquals(OrderStatus.NEW, order.orderStatus());
        assertEquals("ACC001", order.accountId());
    }

    @Test
    @DisplayName("GET /orders/{orderId} returns 404 when order not found")
    void testGetOrderByIdNotFound() {
        var response = orderController.getOrderById("NONEXISTENT");
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("GET /orders/account/{accountId} returns all orders for account")
    void testGetOrdersByAccountIdSuccess() {
        var response = orderController.getOrdersByAccountId("ACC001");
        
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(2, response.getBody().size());
        assertEquals("ORD001", response.getBody().get(0).orderId());
        assertEquals("ORD002", response.getBody().get(1).orderId());
        assertTrue(response.getBody().stream().allMatch(o -> "ACC001".equals(o.accountId())));
    }

    @Test
    @DisplayName("GET /orders/account/{accountId} returns empty list for unknown account")
    void testGetOrdersByAccountIdEmpty() {
        var response = orderController.getOrdersByAccountId("ACC999");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} updates quantity successfully")
    void testUpdateOrderQuantitySuccess() {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(200, null, null, null, null);
        
        var response = orderController.updateOrder("ORD001", updateRequest);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(200, response.getBody().quantity());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} updates price successfully")
    void testUpdateOrderPriceSuccess() {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(null, new BigDecimal("160.50"), null, null, null);
        
        var response = orderController.updateOrder("ORD001", updateRequest);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(new BigDecimal("160.50"), response.getBody().price());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} updates order status successfully")
    void testUpdateOrderStatusSuccess() {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(null, null, null, OrderStatus.FILLED, "system");
        
        var response = orderController.updateOrder("ORD001", updateRequest);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(OrderStatus.FILLED, response.getBody().orderStatus());
        assertEquals("system", response.getBody().updatedBy());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} partial update (only some fields)")
    void testUpdateOrderPartialUpdate() {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(150, new BigDecimal("155.00"), null, null, null);
        
        var response = orderController.updateOrder("ORD001", updateRequest);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(150, response.getBody().quantity());
        assertEquals(new BigDecimal("155.00"), response.getBody().price());
        assertEquals(OrderSide.BUY, response.getBody().side());
        assertEquals(OrderStatus.NEW, response.getBody().orderStatus());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} increments version on update")
    void testUpdateOrderVersionIncrement() {
        int initialVersion = testOrder1.getVersion();
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(120, null, null, null, null);
        
        var response = orderController.updateOrder("ORD001", updateRequest);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(initialVersion + 1, response.getBody().version());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} updates lastUpdated timestamp")
    void testUpdateOrderLastUpdatedTimestamp() {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(120, null, null, null, null);
        
        var response = orderController.updateOrder("ORD001", updateRequest);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody().lastUpdated());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} returns 404 for non-existent order")
    void testUpdateOrderNotFound() {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(200, null, null, null, null);
        
        var response = orderController.updateOrder("NONEXISTENT", updateRequest);
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("DELETE /orders/{orderId} soft-deletes order (sets status CANCELLED)")
    void testDeleteOrderSuccess() {
        var deleteResponse = orderController.deleteOrder("ORD001");
        assertTrue(deleteResponse.getStatusCode().is2xxSuccessful());
        
        var getResponse = orderController.getOrderById("ORD001");
        assertTrue(getResponse.getStatusCode().is2xxSuccessful());
        assertEquals(OrderStatus.CANCELLED, getResponse.getBody().orderStatus());
    }

    @Test
    @DisplayName("DELETE /orders/{orderId} returns 404 for non-existent order")
    void testDeleteOrderNotFound() {
        var response = orderController.deleteOrder("NONEXISTENT");
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("DELETE /orders/{orderId} increments version on soft-delete")
    void testDeleteOrderVersionIncrement() {
        int initialVersion = testOrder1.getVersion();
        
        orderController.deleteOrder("ORD001");
        var getResponse = orderController.getOrderById("ORD001");
        assertEquals(initialVersion + 1, getResponse.getBody().version());
    }

    @Test
    @DisplayName("DELETE /orders/{orderId} updates lastUpdated timestamp")
    void testDeleteOrderLastUpdatedTimestamp() {
        orderController.deleteOrder("ORD001");
        var getResponse = orderController.getOrderById("ORD001");
        assertNotNull(getResponse.getBody().lastUpdated());
    }

    @Test
    @Disabled("Awaiting AccountService/InstrumentService integration")
    @DisplayName("POST /orders creates order with valid request")
    void testPlaceOrderSuccess() {
        // TODO: Enable this test when AccountService and InstrumentService are available
    }
}

/*
================================================================================
DOCKER INTEGRATION TEST VERSION
================================================================================

To convert these unit tests to Docker integration tests:

1. Replace unit test approach with @SpringBootTest
2. Add TestContainers configuration
3. Replace InMemoryOrderRepository with real JPA repository
4. Use actual PostgreSQL database

EXAMPLE CONVERSION (commented out):

import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("OrderController Docker Integration Tests")
public class OrderControllerDockerTest {

    // SETUP: Define PostgreSQL container for testing
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("orders_test_db")
        .withUsername("test_user")
        .withPassword("test_password");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;  // Real JPA repository, not in-memory

    @BeforeEach
    void setUp() {
        // Clear database before each test
        orderRepository.deleteAll();

        // Insert test data directly into PostgreSQL
        Order testOrder1 = new Order(...);
        orderRepository.save(testOrder1);
    }

    // TESTS: Same assertions as unit tests, but against real PostgreSQL via REST
    @Test
    @DisplayName("GET /orders/{orderId} returns order from PostgreSQL")
    void testGetOrderByIdSuccess() {
        ResponseEntity<OrderResponse> response = restTemplate.getForEntity("/orders/ORD001", OrderResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ORD001", response.getBody().orderId());
    }

    // All other tests remain identical in structure
    // Only the request mechanism changes (direct method call → REST call)
}

MAVEN DEPENDENCIES (add to pom.xml):
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>1.19.0</version>
        <scope>test</scope>
    </dependency>

WHEN READY TO ENABLE:
1. Uncomment Docker test class
2. Add TestContainers dependencies to pom.xml
3. Implement real JPA OrderRepository
4. Run: mvn test
5. TestContainers will spin up PostgreSQL container automatically during test execution

BENEFITS OF THIS APPROACH:
- Tests run against real PostgreSQL (same as production)
- No more mocking database layer
- Guarantees schema compatibility
- Catches integration issues early
- Database state fully isolated per test run

================================================================================
 */
