package com.neueda.leap.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.neueda.leap.models.Account;
import com.neueda.leap.exceptions.AccountNotActiveException;
import com.neueda.leap.exceptions.InsufficientFundsException;

public class AccountService {

    public void credit(Account account, BigDecimal amount) throws AccountNotActiveException {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        
        if (!account.isActive()) {
            throw new AccountNotActiveException("Cannot credit an inactive account");
        }
        
        account.setCashBalance(account.getCashBalance().add(amount));
        account.setLastUpdated(LocalDateTime.now());
        account.setVersion(account.getVersion() + 1);
    }

    public void debit(Account account, BigDecimal amount) 
            throws AccountNotActiveException, InsufficientFundsException {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        
        if (!account.isActive()) {
            throw new AccountNotActiveException("Cannot debit an inactive account");
        }
        
        if (account.getCashBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                "Insufficient funds. Balance: " + account.getCashBalance() + 
                ", Requested: " + amount
            );
        }
        
        account.setCashBalance(account.getCashBalance().subtract(amount));
        account.setLastUpdated(LocalDateTime.now());
        account.setVersion(account.getVersion() + 1);
    }
}
