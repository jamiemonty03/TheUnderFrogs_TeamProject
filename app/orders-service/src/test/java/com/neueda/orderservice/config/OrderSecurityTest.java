package com.neueda.orderservice.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.neueda.orderservice.controllers.OrderController;
import com.neueda.orderservice.repositories.OrderRepository;
import com.neueda.orderservice.services.orderServices.OrderProcessor;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderProcessor orderProcessor;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    @DisplayName("Requests without a bearer token are rejected with 401")
    void rejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Requests with a valid JWT reach the controller")
    void allowsRequestsWithJwt() throws Exception {
        Mockito.when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        mockMvc.perform(get("/orders").with(jwt())).andExpect(status().isOk());
    }
}
