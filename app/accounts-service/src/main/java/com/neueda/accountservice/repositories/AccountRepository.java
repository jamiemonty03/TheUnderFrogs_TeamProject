package com.neueda.accountservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.neueda.accountservice.models.Account;

public interface AccountRepository extends JpaRepository<Account, String> {

    @Query(value = "SELECT nextval('account_number_seq')", nativeQuery = true)
    long nextAccountNumber();
}
