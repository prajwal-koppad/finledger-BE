package com.fintech.ledger.repository;

import com.fintech.ledger.entities.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("""
        select coalesce(sum(case when le.entryType = 'DEBIT' then le.amountMinor else 0 end), 0)
        from LedgerEntry le
        where le.account.id = :accountId
    """)
    Long sumDebitsByAccountId(Long accountId);

    @Query("""
        select coalesce(sum(case when le.entryType = 'CREDIT' then le.amountMinor else 0 end), 0)
        from LedgerEntry le
        where le.account.id = :accountId
    """)
    Long sumCreditsByAccountId(Long accountId);
}