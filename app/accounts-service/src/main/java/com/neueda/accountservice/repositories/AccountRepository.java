package com.neueda.accountservice.repositories;

import java.util.Optional;
import com.neueda.accountservice.models.Account;
import com.neueda.accountservice.exceptions.AccountNotFoundException;

public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(String accountId);
    boolean delete(String accountId);
    boolean exists(String accountId);
}
