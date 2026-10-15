package com.neueda.accountservice.controllers;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.neueda.accountservice.config.TestSecurityConfig;
import com.neueda.accountservice.exceptions.AccountAuthorizationException;
import com.neueda.accountservice.exceptions.AccountNotFoundException;
import com.neueda.accountservice.exceptions.PaymentGatewayException;
import com.neueda.accountservice.exceptions.PaymentsUnavailableException;
import com.neueda.accountservice.services.TopUpService;
import com.neueda.accountservice.utils.AuthorizationUtils;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(TopUpController.class)
@Import(TestSecurityConfig.class)
@DisplayName("POST /accounts/{accountId}/top-ups")
class TopUpControllerTest {

    private static final String URL = "/accounts/ACC0011/top-ups";
    private static final String CHECKOUT_URL = "https://checkout.stripe.com/c/pay/cs_test_123";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TopUpService topUpService;

    private MockedStatic<AuthorizationUtils> authorization;

    @BeforeEach
    void setUp() {
        authorization = mockStatic(AuthorizationUtils.class);
        authorization.when(() -> AuthorizationUtils.verifyAccountAccess(anyString())).thenAnswer(invocation -> null);
    }

    @AfterEach
    void tearDown() {
        authorization.close();
    }

    private ResultActions startTopUp(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    @DisplayName("Returns Stripe's checkout URL for a valid amount")
    void startsTopUp() throws Exception {
        when(topUpService.startTopUp("ACC0011", new BigDecimal("250.00"), "/positions")).thenReturn(CHECKOUT_URL);

        startTopUp("{\"amount\": 250.00, \"returnTo\": \"/positions\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.checkoutUrl", equalTo(CHECKOUT_URL)));
    }

    @Test
    @DisplayName("Accepts the exact limits of $10.00 and $10,000.00")
    void acceptsLimits() throws Exception {
        when(topUpService.startTopUp(eq("ACC0011"), any(), any())).thenReturn(CHECKOUT_URL);

        startTopUp("{\"amount\": 10.00}").andExpect(status().isOk());
        startTopUp("{\"amount\": 10000.00}").andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"amount\": 9.99}",
        "{\"amount\": 10000.01}",
        "{\"amount\": 0}",
        "{\"amount\": -50}",
        "{\"amount\": 25.555}",
        "{}"
    })
    @DisplayName("Rejects amounts outside $10 to $10,000, or with more than 2 decimals, with 422")
    void rejectsInvalidAmounts(String body) throws Exception {
        startTopUp(body)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errorCode", equalTo("VAL-422")));

        verifyNoInteractions(topUpService);
    }

    @Test
    @DisplayName("Refuses to start a top-up for someone else's account with 403")
    void otherAccount() throws Exception {
        authorization.when(() -> AuthorizationUtils.verifyAccountAccess("ACC0011"))
            .thenThrow(new AccountAuthorizationException("User is not authorized to access account: ACC0011"));

        startTopUp("{\"amount\": 50}")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode", equalTo("AUTH-403")));

        verifyNoInteractions(topUpService);
    }

    @Test
    @DisplayName("Answers 503 when Stripe is not configured")
    void unavailable() throws Exception {
        when(topUpService.startTopUp(any(), any(), any())).thenThrow(new PaymentsUnavailableException());

        startTopUp("{\"amount\": 50}")
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.errorCode", equalTo("PAY-503")))
            .andExpect(jsonPath("$.message", equalTo("Adding funds isn't available right now")));
    }

    @Test
    @DisplayName("Answers 502 with a plain message when Stripe fails, without Stripe's details")
    void stripeFails() throws Exception {
        when(topUpService.startTopUp(any(), any(), any()))
            .thenThrow(new PaymentGatewayException("Could not start the payment", new RuntimeException("Invalid API Key provided: sk_test_***")));

        startTopUp("{\"amount\": 50}")
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.errorCode", equalTo("PAY-502")))
            .andExpect(jsonPath("$.message", equalTo("Could not start the payment. Please try again.")));
    }

    @Test
    @DisplayName("Answers 404 when the account does not exist")
    void accountNotFound() throws Exception {
        when(topUpService.startTopUp(any(), any(), any())).thenThrow(new AccountNotFoundException("Account not found: ACC0011"));

        startTopUp("{\"amount\": 50}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode", equalTo("ACC-404")));
    }

    @Test
    @DisplayName("Passes the amount and returnTo through to the service unchanged")
    void passesThrough() throws Exception {
        when(topUpService.startTopUp(any(), any(), any())).thenReturn(CHECKOUT_URL);

        startTopUp("{\"amount\": 99.99, \"returnTo\": \"/orders?status=NEW\"}").andExpect(status().isOk());

        verify(topUpService).startTopUp("ACC0011", new BigDecimal("99.99"), "/orders?status=NEW");
    }
}
