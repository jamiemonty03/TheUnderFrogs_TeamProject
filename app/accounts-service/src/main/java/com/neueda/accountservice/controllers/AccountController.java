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

    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccount(@PathVariable String accountId) throws AccountNotFoundException {
        Account account = accountService.getAccountById(accountId);
        return ResponseEntity.ok(account);
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<Account> updateAccount(
            @PathVariable String accountId,
            @Valid @RequestBody Account account) throws AccountNotFoundException {
        Account updated = accountService.updateAccount(accountId, account);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountId) throws AccountNotFoundException {
        accountService.deleteAccount(accountId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{accountId}/credit")
    public ResponseEntity<Account> creditAccount(
            @PathVariable String accountId,
            @RequestBody Map<String, BigDecimal> request) throws AccountNotFoundException, AccountNotActiveException {
        BigDecimal amount = request.get("amount");
        Account updated = accountService.credit(accountId, amount);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{accountId}/debit")
    public ResponseEntity<Account> debitAccount(
            @PathVariable String accountId,
            @RequestBody Map<String, BigDecimal> request) throws AccountNotFoundException, AccountNotActiveException, InsufficientFundsException {
        BigDecimal amount = request.get("amount");
        Account updated = accountService.debit(accountId, amount);
        return ResponseEntity.ok(updated);
    }
}

