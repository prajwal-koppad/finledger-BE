package com.fintech.ledger.repository;

import com.fintech.ledger.entities.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
}