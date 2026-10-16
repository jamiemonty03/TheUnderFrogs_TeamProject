package com.neueda.orderservice.controllers;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neueda.orderservice.config.SecurityConfig;
import com.neueda.orderservice.repositories.OrderRepository;
import com.neueda.orderservice.services.OrderCancellationService;
import com.neueda.orderservice.services.OrderStatusService;
import com.neueda.orderservice.services.orderServices.OrderProcessor;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderValidationMvcTest {

    private static final String VALID_ORDER = """
            {"accountId": "ACC0001", "symbol": "AAPL", "side": "BUY",
             "quantity": 10, "priceLimit": 150.00, "idempotencyKey": "key-1"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            missing accountId      | accountId      | Account ID is required
            missing symbol         | symbol         | Symbol is required
            missing side           | side           | Order side (BUY/SELL) is required
            missing quantity       | quantity       | Quantity is required
            missing priceLimit     | priceLimit     | Price limit is required
            missing idempotencyKey | idempotencyKey | Idempotency key is required
            """)
    @DisplayName("POST /orders with a missing required field returns 422 VAL-422")
    void missingFieldReturns422(String caseName, String field, String expectedMessage) throws Exception {
        ObjectNode order = (ObjectNode) objectMapper.readTree(VALID_ORDER);
        order.remove(field);

        mockMvc.perform(post("/orders").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VAL-422"))
                .andExpect(jsonPath("$.message").value(expectedMessage));

        verifyNoInteractions(orderProcessor);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            zero quantity     | "quantity": 0     | Quantity must be greater than 0
            negative quantity | "quantity": -5    | Quantity must be greater than 0
            zero priceLimit     | "priceLimit": 0     | Price limit must be greater than 0
            negative priceLimit | "priceLimit": -1.50 | Price limit must be greater than 0
            blank accountId   | "accountId": "  " | Account ID is required
            """)
    @DisplayName("POST /orders with an out-of-range value returns 422 VAL-422")
    void invalidValueReturns422(String caseName, String replacement, String expectedMessage) throws Exception {
        mockMvc.perform(post("/orders").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(withField(replacement)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VAL-422"))
                .andExpect(jsonPath("$.message").value(expectedMessage));

        verifyNoInteractions(orderProcessor);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            side not BUY or SELL | "side": "HOLD"    | Invalid value for field 'side'
            quantity is text     | "quantity": "ten" | Invalid value for field 'quantity'
            priceLimit is text   | "priceLimit": "cheap" | Invalid value for field 'priceLimit'
            """)
    @DisplayName("POST /orders with a wrong type or unknown enum returns 422 VAL-422 naming the field")
    void mistypedValueReturns422(String caseName, String replacement, String expectedMessage) throws Exception {
        mockMvc.perform(post("/orders").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(withField(replacement)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VAL-422"))
                .andExpect(jsonPath("$.message").value(expectedMessage));

        verifyNoInteractions(orderProcessor);
    }

    @Test
    @DisplayName("POST /orders with broken JSON returns 422 VAL-422")
    void malformedJsonReturns422() throws Exception {
        mockMvc.perform(post("/orders").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\": \"ACC0001\","))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VAL-422"))
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verifyNoInteractions(orderProcessor);
    }

    @Test
    @DisplayName("POST /orders with no body returns 422 VAL-422")
    void emptyBodyReturns422() throws Exception {
        mockMvc.perform(post("/orders").with(jwt()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VAL-422"));

        verifyNoInteractions(orderProcessor);
    }

    @Test
    @DisplayName("PATCH /orders/{id}/status without newStatus returns 422 VAL-422")
    void statusChangeWithoutNewStatusReturns422() throws Exception {
        mockMvc.perform(patch("/orders/order-1/status").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\": \"NEW\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VAL-422"));

        verifyNoInteractions(orderStatusService);
    }

    @Test
    @DisplayName("PATCH /orders/{id}/status with an unknown status returns 422 VAL-422")
    void statusChangeWithUnknownStatusReturns422() throws Exception {
        mockMvc.perform(patch("/orders/order-1/status").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\": \"NEW\", \"newStatus\": \"SHIPPED\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VAL-422"))
                .andExpect(jsonPath("$.message").value("Invalid value for field 'newStatus'"));

        verifyNoInteractions(orderStatusService);
    }

    @Test
    @DisplayName("PUT /orders/{id} with orderStatus returns 422 VAL-422")
    void putWithOrderStatusReturns422() throws Exception {
        mockMvc.perform(put("/orders/order-1").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderStatus\": \"FILLED\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VAL-422"));
    }

    private static String withField(String replacement) {
        String field = replacement.substring(1, replacement.indexOf('"', 1));
        return VALID_ORDER.replaceAll("\"" + field + "\": [^,}]+", replacement);
    }
}
