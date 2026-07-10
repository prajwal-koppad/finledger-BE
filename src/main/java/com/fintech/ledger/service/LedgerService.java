package com.fintech.ledger.service;

import com.fintech.ledger.dto.request.CreateLedgerTransactionRequest;
import com.fintech.ledger.dto.response.LedgerTransactionResponse;
import com.fintech.ledger.entities.Account;
import com.fintech.ledger.entities.JournalEntry;
import com.fintech.ledger.entities.LedgerEntry;
import com.fintech.ledger.enums.EntryType;
import com.fintech.ledger.enums.ReferenceType;
import com.fintech.ledger.exception.NotFoundException;
import com.fintech.ledger.exception.ValidationException;
import com.fintech.ledger.repository.AccountRepository;
import com.fintech.ledger.repository.JournalEntryRepository;
import com.fintech.ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountService accountService;

    @Transactional
    public LedgerTransactionResponse recordTransaction(CreateLedgerTransactionRequest request) {
        if (request.getDebitAccountId().equals(request.getCreditAccountId())) {
            throw new ValidationException("Debit and credit accounts cannot be the same");
        }

        Account debitAccount = accountRepository.findById(request.getDebitAccountId())
                .orElseThrow(() -> new NotFoundException("Debit account not found"));

        Account creditAccount = accountRepository.findById(request.getCreditAccountId())
                .orElseThrow(() -> new NotFoundException("Credit account not found"));

        JournalEntry journalEntry = recordSystemTransaction(
                debitAccount,
                creditAccount,
                request.getAmountMinor(),
                request.getReferenceId(),
                request.getDescription(),
                ReferenceType.TRANSFER
        );

        return LedgerTransactionResponse.builder()
                .journalEntryId(journalEntry.getId())
                .referenceId(request.getReferenceId())
                .message("Transaction recorded successfully")
                .build();
    }

    @Transactional
    public JournalEntry recordSystemTransaction(Account debitAccount, Account creditAccount, Long amountMinor, String referenceId, String description, ReferenceType referenceType) {
        if (!debitAccount.getCurrency().equalsIgnoreCase(creditAccount.getCurrency())) {
            throw new ValidationException("Cross-currency transactions are not supported in this version");
        }

        // Ledger and Journal entries must be immutable.
        // We never UPDATE these rows, we only INSERT. 
        // If a transaction needs to be reversed, we'd add a new compensating transaction.
        JournalEntry journalEntry = journalEntryRepository.save(
                JournalEntry.builder()
                        .referenceId(referenceId)
                        .referenceType(referenceType)
                        .description(description)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        ledgerEntryRepository.save(
                LedgerEntry.builder()
                        .journalEntry(journalEntry)
                        .account(debitAccount)
                        .entryType(EntryType.DEBIT)
                        .amountMinor(amountMinor)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        ledgerEntryRepository.save(
                LedgerEntry.builder()
                        .journalEntry(journalEntry)
                        .account(creditAccount)
                        .entryType(EntryType.CREDIT)
                        .amountMinor(amountMinor)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return journalEntry;
    }
}