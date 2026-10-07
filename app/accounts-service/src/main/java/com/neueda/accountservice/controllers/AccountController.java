package com.neueda.accountservice.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.neueda.accountservice.models.Account;
import com.neueda.accountservice.services.AccountService;
import com.neueda.accountservice.exceptions.AccountNotFoundException;
import com.neueda.accountservice.exceptions.AccountNotActiveException;
import com.neueda.accountservice.exceptions.InsufficientFundsException;
import com.neueda.accountservice.dtos.requests.CashMovementRequest;
import com.neueda.accountservice.dtos.requests.ReversalRequest;
import com.neueda.accountservice.utils.AuthorizationUtils;


@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@Valid @RequestBody Account account) {
        Account created = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        List<Account> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccount(@PathVariable String accountId) throws AccountNotFoundException {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        Account account = accountService.getAccountById(accountId);
        return ResponseEntity.ok(account);
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<Account> updateAccount(
            @PathVariable String accountId,
            @Valid @RequestBody Account account) throws AccountNotFoundException {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        Account updated = accountService.updateAccount(accountId, account);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountId) throws AccountNotFoundException {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        accountService.deleteAccount(accountId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{accountId}/credit")
    public ResponseEntity<Account> creditAccount(
            @PathVariable String accountId,
            @RequestBody CashMovementRequest request) throws AccountNotFoundException, AccountNotActiveException {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        Account updated = request.orderId() == null
            ? accountService.credit(accountId, request.amount())
            : accountService.credit(accountId, request.amount(), request.orderId());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{accountId}/debit")
    public ResponseEntity<Account> debitAccount(
            @PathVariable String accountId,
            @RequestBody CashMovementRequest request) throws AccountNotFoundException, AccountNotActiveException, InsufficientFundsException {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        Account updated = request.orderId() == null
            ? accountService.debit(accountId, request.amount())
            : accountService.debit(accountId, request.amount(), request.orderId());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{accountId}/reversal")
    public ResponseEntity<Account> reverseMovement(
            @PathVariable String accountId,
            @Valid @RequestBody ReversalRequest request) throws AccountNotFoundException, InsufficientFundsException {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        return ResponseEntity.ok(accountService.reverse(accountId, request.orderId()));
    }

}

