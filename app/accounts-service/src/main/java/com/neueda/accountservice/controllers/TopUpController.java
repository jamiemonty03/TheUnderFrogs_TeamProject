package com.neueda.accountservice.controllers;

import com.neueda.accountservice.dtos.requests.TopUpRequest;
import com.neueda.accountservice.dtos.responses.TopUpConfirmResponse;
import com.neueda.accountservice.dtos.responses.TopUpStartResponse;
import com.neueda.accountservice.exceptions.AccountAuthorizationException;
import com.neueda.accountservice.exceptions.AccountNotActiveException;
import com.neueda.accountservice.exceptions.AccountNotFoundException;
import com.neueda.accountservice.services.TopUpService;
import com.neueda.accountservice.utils.AuthorizationUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts/{accountId}/top-ups")
public class TopUpController {

    private final TopUpService topUpService;

    public TopUpController(TopUpService topUpService) {
        this.topUpService = topUpService;
    }

    @PostMapping
    public ResponseEntity<TopUpStartResponse> startTopUp(
            @PathVariable String accountId,
            @Valid @RequestBody TopUpRequest request)
            throws AccountAuthorizationException, AccountNotFoundException, AccountNotActiveException {
        AuthorizationUtils.verifyAccountAccess(accountId);
        String checkoutUrl = topUpService.startTopUp(accountId, request.amount(), request.returnTo());
        return ResponseEntity.ok(new TopUpStartResponse(checkoutUrl));
    }

    @PostMapping("/{sessionId}/confirm")
    public ResponseEntity<TopUpConfirmResponse> confirmTopUp(
            @PathVariable String accountId,
            @PathVariable String sessionId)
            throws AccountAuthorizationException, AccountNotFoundException, AccountNotActiveException {
        AuthorizationUtils.verifyAccountAccess(accountId);
        return ResponseEntity.ok(topUpService.confirmTopUp(accountId, sessionId));
    }
}
