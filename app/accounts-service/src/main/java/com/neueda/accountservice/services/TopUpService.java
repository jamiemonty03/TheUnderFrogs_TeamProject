package com.neueda.accountservice.services;

import com.neueda.accountservice.clients.CheckoutRequest;
import com.neueda.accountservice.clients.CheckoutSession;
import com.neueda.accountservice.clients.PaymentGateway;
import com.neueda.accountservice.dtos.responses.TopUpConfirmResponse;
import com.neueda.accountservice.enums.AccountStatus;
import com.neueda.accountservice.exceptions.AccountAuthorizationException;
import com.neueda.accountservice.exceptions.AccountNotActiveException;
import com.neueda.accountservice.exceptions.AccountNotFoundException;
import com.neueda.accountservice.exceptions.PaymentNotFoundException;
import com.neueda.accountservice.exceptions.PaymentsUnavailableException;
import com.neueda.accountservice.exceptions.TopUpNotPaidException;
import com.neueda.accountservice.models.Account;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TopUpService {

    static final String DEFAULT_RETURN_TO = "/dashboard";
    static final String MOVEMENT_KEY_PREFIX = "stripe:";
    private static final String CURRENCY = "usd";
    private static final Pattern SESSION_ID = Pattern.compile("^cs_(test|live)_[A-Za-z0-9]{1,200}$");
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

    public TopUpConfirmResponse confirmTopUp(String accountId, String sessionId)
            throws AccountNotFoundException, AccountNotActiveException, AccountAuthorizationException {
        if (!paymentGateway.isAvailable()) {
            throw new PaymentsUnavailableException();
        }
        if (sessionId == null || !SESSION_ID.matcher(sessionId).matches()) {
            throw new PaymentNotFoundException();
        }

        CheckoutSession session = paymentGateway.retrieveCheckout(sessionId);
        if (!accountId.equals(session.accountId())) {
            throw new AccountAuthorizationException("This payment belongs to a different account");
        }
        if (!session.isPaid()) {
            throw new TopUpNotPaidException("This payment hasn't been completed");
        }
        if (!CURRENCY.equals(session.currency())
                || session.amountTotalCents() == null
                || session.amountTotalCents() <= 0
                || session.paymentIntentId() == null) {
            throw new TopUpNotPaidException("This payment can't be added to your account");
        }

        BigDecimal amount = BigDecimal.valueOf(session.amountTotalCents(), 2);
        Account account = accountService.credit(accountId, amount, MOVEMENT_KEY_PREFIX + session.paymentIntentId());
        return new TopUpConfirmResponse(amount, account.getCashBalance());
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
