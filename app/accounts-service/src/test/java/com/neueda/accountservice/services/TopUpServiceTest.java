package com.neueda.accountservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.neueda.accountservice.clients.CheckoutRequest;
import com.neueda.accountservice.clients.CheckoutSession;
import com.neueda.accountservice.clients.PaymentGateway;
import com.neueda.accountservice.enums.AccountStatus;
import com.neueda.accountservice.exceptions.AccountNotActiveException;
import com.neueda.accountservice.exceptions.AccountNotFoundException;
import com.neueda.accountservice.exceptions.PaymentsUnavailableException;
import com.neueda.accountservice.models.Account;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class TopUpServiceTest {

    private static final String CHECKOUT_URL = "https://checkout.stripe.com/c/pay/cs_test_123";

    private AccountService accountService;
    private PaymentGateway paymentGateway;
    private TopUpService topUpService;

    @BeforeEach
    void setUp() throws Exception {
        accountService = mock(AccountService.class);
        paymentGateway = mock(PaymentGateway.class);
        topUpService = new TopUpService(accountService, paymentGateway, "http://localhost:4200/");
        when(paymentGateway.isAvailable()).thenReturn(true);
        when(accountService.getAccountById("ACC0011"))
            .thenReturn(new Account("ACC0011", 11L, "Demo Trader", new BigDecimal("500.00"), AccountStatus.ACTIVE));
        when(paymentGateway.createCheckout(any()))
            .thenReturn(new CheckoutSession("cs_test_123", CHECKOUT_URL, "unpaid", null, 25000L, "usd", "ACC0011"));
    }

    private CheckoutRequest sentRequest() {
        ArgumentCaptor<CheckoutRequest> captor = ArgumentCaptor.forClass(CheckoutRequest.class);
        verify(paymentGateway).createCheckout(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Creates a checkout for the amount in cents and returns Stripe's payment page URL")
    void startsCheckout() throws Exception {
        String url = topUpService.startTopUp("ACC0011", new BigDecimal("250.00"), "/positions");

        CheckoutRequest request = sentRequest();
        assertThat(url).isEqualTo(CHECKOUT_URL);
        assertThat(request.accountId()).isEqualTo("ACC0011");
        assertThat(request.amountCents()).isEqualTo(25000L);
        assertThat(request.successUrl())
            .isEqualTo("http://localhost:4200/wallet/return?session_id={CHECKOUT_SESSION_ID}&returnTo=%2Fpositions");
        assertThat(request.cancelUrl())
            .isEqualTo("http://localhost:4200/wallet/return?cancelled=1&returnTo=%2Fpositions");
    }

    @Test
    @DisplayName("Keeps a returnTo with a query string, encoded so it cannot break the return URL")
    void encodesReturnTo() throws Exception {
        topUpService.startTopUp("ACC0011", new BigDecimal("10.00"), "/orders?status=NEW&page=2");

        assertThat(sentRequest().successUrl()).endsWith("&returnTo=%2Forders%3Fstatus%3DNEW%26page%3D2");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "https://evil.example.com",
        "//evil.example.com",
        "/\\evil.example.com",
        "javascript:alert(1)",
        "orders",
        "/login",
        "/register",
        "/wallet/return?session_id=x",
        "/orders\nSet-Cookie: x=1"
    })
    @DisplayName("Sends the user back to the dashboard instead of an unsafe or looping returnTo")
    void rejectsUnsafeReturnTo(String returnTo) throws Exception {
        topUpService.startTopUp("ACC0011", new BigDecimal("10.00"), returnTo);

        assertThat(sentRequest().successUrl()).endsWith("&returnTo=%2Fdashboard");
    }

    @ParameterizedTest
    @CsvSource({"10, 1000", "250.5, 25050", "99.99, 9999", "10000.00, 1000000"})
    @DisplayName("Converts dollars to cents exactly")
    void convertsToCents(String dollars, long cents) {
        assertThat(TopUpService.toCents(new BigDecimal(dollars))).isEqualTo(cents);
    }

    @Test
    @DisplayName("Without Stripe configured, refuses before touching the account or Stripe")
    void unavailable() {
        when(paymentGateway.isAvailable()).thenReturn(false);

        assertThatThrownBy(() -> topUpService.startTopUp("ACC0011", new BigDecimal("50"), "/dashboard"))
            .isInstanceOf(PaymentsUnavailableException.class);
        verifyNoInteractions(accountService);
        verify(paymentGateway, never()).createCheckout(any());
    }

    @Test
    @DisplayName("Does not start a payment for an account that does not exist")
    void accountNotFound() throws Exception {
        when(accountService.getAccountById("ACC9999")).thenThrow(new AccountNotFoundException("Account not found: ACC9999"));

        assertThatThrownBy(() -> topUpService.startTopUp("ACC9999", new BigDecimal("50"), "/dashboard"))
            .isInstanceOf(AccountNotFoundException.class);
        verify(paymentGateway, never()).createCheckout(any());
    }

    @Test
    @DisplayName("Does not start a payment for a suspended account")
    void accountNotActive() throws Exception {
        when(accountService.getAccountById("ACC0011"))
            .thenReturn(new Account("ACC0011", 11L, "Demo Trader", BigDecimal.ZERO, AccountStatus.SUSPENDED));

        assertThatThrownBy(() -> topUpService.startTopUp("ACC0011", new BigDecimal("50"), "/dashboard"))
            .isInstanceOf(AccountNotActiveException.class);
        verify(paymentGateway, never()).createCheckout(any());
    }
}
