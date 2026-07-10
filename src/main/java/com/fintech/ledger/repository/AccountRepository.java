package com.fintech.ledger.repository;

import com.fintech.ledger.entities.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountCode(String accountCode);
}