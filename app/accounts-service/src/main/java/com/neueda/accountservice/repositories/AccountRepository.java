package com.neueda.accountservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.neueda.accountservice.models.Account;

public interface AccountRepository extends JpaRepository<Account, String> {}
