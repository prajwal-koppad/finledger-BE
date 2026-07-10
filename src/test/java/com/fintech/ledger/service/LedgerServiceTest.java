package com.fintech.ledger.service;

import com.fintech.ledger.dto.request.CreateLedgerTransactionRequest;
import com.fintech.ledger.dto.response.LedgerTransactionResponse;
import com.fintech.ledger.entities.Account;
import com.fintech.ledger.entities.JournalEntry;
import com.fintech.ledger.entities.LedgerEntry;
import com.fintech.ledger.enums.AccountType;
import com.fintech.ledger.enums.EntryType;
import com.fintech.ledger.enums.ReferenceType;
import com.fintech.ledger.exception.ValidationException;
import com.fintech.ledger.repository.AccountRepository;
import com.fintech.ledger.repository.JournalEntryRepository;
import com.fintech.ledger.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private LedgerService ledgerService;

    private Account debitAccount;
    private Account creditAccount;

    @BeforeEach
    void setUp() {
        debitAccount = Account.builder()
                .id(1L)
                .accountCode("CASH")
                .name("Cash Account")
                .type(AccountType.ASSET)
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .build();

        creditAccount = Account.builder()
                .id(2L)
                .accountCode("REVENUE")
                .name("Revenue Account")
                .type(AccountType.REVENUE)
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void recordSystemTransaction_ShouldSaveDoubleEntry() {
        // Arrange
        JournalEntry mockJournalEntry = JournalEntry.builder().id(100L).build();
        when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(mockJournalEntry);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        JournalEntry result = ledgerService.recordSystemTransaction(
                debitAccount,
                creditAccount,
                5000L,
                "REF-123",
                "Test transaction",
                ReferenceType.TRANSFER
        );

        // Assert
        assertNotNull(result);
        
        ArgumentCaptor<LedgerEntry> ledgerEntryCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(ledgerEntryCaptor.capture());
        
        List<LedgerEntry> savedEntries = ledgerEntryCaptor.getAllValues();
        assertEquals(2, savedEntries.size());
        
        // Verify Debit Entry
        LedgerEntry debitEntry = savedEntries.stream()
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .findFirst().orElseThrow();
        assertEquals(debitAccount, debitEntry.getAccount());
        assertEquals(5000L, debitEntry.getAmountMinor());
        
        // Verify Credit Entry
        LedgerEntry creditEntry = savedEntries.stream()
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .findFirst().orElseThrow();
        assertEquals(creditAccount, creditEntry.getAccount());
        assertEquals(5000L, creditEntry.getAmountMinor());
    }

    @Test
    void recordTransaction_SameAccount_ThrowsException() {
        CreateLedgerTransactionRequest request = new CreateLedgerTransactionRequest();
        request.setDebitAccountId(1L);
        request.setCreditAccountId(1L);

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            ledgerService.recordTransaction(request);
        });
        
        assertEquals("Debit and credit accounts cannot be the same", ex.getMessage());
    }

    @Test
    void recordSystemTransaction_CurrencyMismatch_ThrowsException() {
        creditAccount.setCurrency("EUR"); // Mismatch currency

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            ledgerService.recordSystemTransaction(
                    debitAccount, creditAccount, 500L, "REF", "Desc", ReferenceType.TRANSFER
            );
        });

        assertEquals("Cross-currency transactions are not supported in this version", ex.getMessage());
    }
}
