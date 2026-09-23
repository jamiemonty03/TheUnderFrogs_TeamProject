package com.neueda.accountservice.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.neueda.accountservice.models.Account;
import com.neueda.accountservice.repositories.AccountRepository;
import com.neueda.accountservice.enums.AccountStatus;
import com.neueda.accountservice.exceptions.AccountNotActiveException;
import com.neueda.accountservice.exceptions.InsufficientFundsException;
import com.neueda.accountservice.exceptions.AccountNotFoundException;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        
        if (account.getCreatedAt() == null) {
            account.setCreatedAt(LocalDateTime.now());
        }
        if (account.getLastUpdated() == null) {
            account.setLastUpdated(LocalDateTime.now());
        }
        if (account.getVersion() == 0) {
            account.setVersion(1);
        }
        
        accountRepository.save(account);
        return account;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(String accountId) throws AccountNotFoundException {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
    }

    public Account updateAccount(String accountId, Account updatedAccount) throws AccountNotFoundException {
        Account existing = getAccountById(accountId);
        
        if (updatedAccount.getHolderName() != null && !updatedAccount.getHolderName().trim().isEmpty()) {
            existing.setHolderName(updatedAccount.getHolderName());
        }
        if (updatedAccount.getStatus() != null) {
            existing.setStatus(updatedAccount.getStatus());
        }
        
        existing.setLastUpdated(LocalDateTime.now());
        existing.setVersion(existing.getVersion() + 1);
        
        accountRepository.update(existing);
        return existing;
    }

    public void deleteAccount(String accountId) throws AccountNotFoundException {
        if (!accountRepository.exists(accountId)) {
            throw new AccountNotFoundException("Account not found: " + accountId);
        }
        accountRepository.delete(accountId);
    }

    public Account credit(String accountId, BigDecimal amount) throws AccountNotActiveException, AccountNotFoundException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        
        Account account = getAccountById(accountId);
        
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Cannot credit an inactive account");
        }
        
        account.setCashBalance(account.getCashBalance().add(amount));
        account.setLastUpdated(LocalDateTime.now());
        account.setVersion(account.getVersion() + 1);
        
        accountRepository.update(account);
        return account;
    }

    public Account debit(String accountId, BigDecimal amount) 
            throws AccountNotActiveException, InsufficientFundsException, AccountNotFoundException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        
        Account account = getAccountById(accountId);
        
        if (account.getStatus() != AccountStatus.ACTIVE) {
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
        
        accountRepository.update(account);
        return account;
    }
}
