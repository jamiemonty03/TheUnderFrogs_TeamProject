package com.neueda.leap.repositories;

import java.util.Optional;
import com.neueda.leap.models.Account;
import com.neueda.leap.exceptions.AccountNotFoundException;

public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(String accountId);
    boolean delete(String accountId);
    boolean exists(String accountId);
}
