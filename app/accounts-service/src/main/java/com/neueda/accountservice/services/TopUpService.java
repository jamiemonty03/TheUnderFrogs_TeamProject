package com.neueda.accountservice.services;

import com.neueda.accountservice.clients.CheckoutRequest;
import com.neueda.accountservice.clients.PaymentGateway;
import com.neueda.accountservice.enums.AccountStatus;
import com.neueda.accountservice.exceptions.AccountNotActiveException;
import com.neueda.accountservice.exceptions.AccountNotFoundException;
import com.neueda.accountservice.exceptions.PaymentsUnavailableException;
import com.neueda.accountservice.models.Account;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TopUpService {

    static final String DEFAULT_RETURN_TO = "/dashboard";
    private static final List<String> BLOCKED_RETURN_PREFIXES = List.of("/wallet", "/login", "/register");

    private final AccountService accountService;
    private final PaymentGateway paymentGateway;
    private final String frontendUrl;

    public TopUpService(
            AccountService accountService,
            PaymentGateway paymentGateway,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.accountService = accountService;
        this.paymentGateway = paymentGateway;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public String startTopUp(String accountId, BigDecimal amount, String returnTo)
            throws AccountNotFoundException, AccountNotActiveException {
        if (!paymentGateway.isAvailable()) {
            throw new PaymentsUnavailableException();
        }
        Account account = accountService.getAccountById(accountId);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(accountId, account.getStatus().name());
        }

        String back = URLEncoder.encode(safeReturnTo(returnTo), StandardCharsets.UTF_8);
        CheckoutRequest request = new CheckoutRequest(
            accountId,
            toCents(amount),
            frontendUrl + "/wallet/return?session_id={CHECKOUT_SESSION_ID}&returnTo=" + back,
            frontendUrl + "/wallet/return?cancelled=1&returnTo=" + back
        );
        return paymentGateway.createCheckout(request).url();
    }

    static long toCents(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
    }

    static String safeReturnTo(String returnTo) {
        if (returnTo == null
                || !returnTo.startsWith("/")
                || returnTo.startsWith("//")
                || returnTo.contains("\\")
                || returnTo.chars().anyMatch(Character::isISOControl)
                || BLOCKED_RETURN_PREFIXES.stream().anyMatch(returnTo::startsWith)) {
            return DEFAULT_RETURN_TO;
        }
        return returnTo;
    }
}
