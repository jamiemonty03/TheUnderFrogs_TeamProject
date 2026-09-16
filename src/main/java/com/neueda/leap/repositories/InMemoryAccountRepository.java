package com.neueda.leap.repositories;

import java.util.Map;
import java.util.Optional;
import com.neueda.leap.models.Account;
import com.neueda.leap.exceptions.AccountNotFoundException;
import java.util.HashMap;

public class InMemoryAccountRepository implements AccountRepository {
    
    private final Map<String, Account> accounts = new HashMap<>();

    @Override
    public Account save(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        if (account.getAccountId() == null || account.getAccountId().trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        
        accounts.put(account.getAccountId(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(accounts.get(accountId));
    }

    @Override
    public boolean delete(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return false;
        }
        return accounts.remove(accountId) != null;
    }

    @Override
    public boolean exists(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return false;
        }
        return accounts.containsKey(accountId);
    }

    public void clear() {
        accounts.clear();
    }

    public int count() {
        return accounts.size();
    }
}
