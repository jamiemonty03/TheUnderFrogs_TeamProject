package com.neueda.accountservice.repositories;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.accountservice.models.Account;

@Mapper
public interface AccountRepository {
    void save(Account account);
    Optional<Account> findById(String accountId);
    void delete(String accountId);
    boolean exists(String accountId);
    void update(Account account);
}
