package com.neueda.accountservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.neueda.accountservice.models.Account;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByUserId(Long userId);
}
