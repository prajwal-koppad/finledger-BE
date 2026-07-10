package com.fintech.ledger.service;

import com.fintech.ledger.dto.response.AccountBalanceResponse;
import com.fintech.ledger.entities.Account;
import com.fintech.ledger.enums.AccountType;
import com.fintech.ledger.repository.AccountRepository;
import com.fintech.ledger.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .accountCode("CASH")
                .name("Cash")
                .type(AccountType.ASSET)
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getBalance_ShouldCalculateFromLedgerEntries() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        
        // Let's say there are 15000 in debits and 5000 in credits
        when(ledgerEntryRepository.sumDebitsByAccountId(1L)).thenReturn(15000L);
        when(ledgerEntryRepository.sumCreditsByAccountId(1L)).thenReturn(5000L);

        // Act
        AccountBalanceResponse response = accountService.getBalance(1L);

        // Assert
        // balance = debits (15000) - credits (5000) = 10000
        assertEquals(10000L, response.getBalanceMinor());
        assertEquals("USD", response.getCurrency());
        assertEquals("CASH", response.getAccountCode());
    }
    @Test
    void getBalance_LiabilityAccount_ShouldCalculateFromCreditsMinusDebits() {
        // Arrange
        Account liabilityAccount = Account.builder()
                .id(2L)
                .accountCode("LOAN")
                .name("Bank Loan")
                .type(AccountType.LIABILITY)
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .build();
                
        when(accountRepository.findById(2L)).thenReturn(Optional.of(liabilityAccount));
        
        // 0 debits and 5000 credits
        when(ledgerEntryRepository.sumDebitsByAccountId(2L)).thenReturn(0L);
        when(ledgerEntryRepository.sumCreditsByAccountId(2L)).thenReturn(5000L);

        // Act
        AccountBalanceResponse response = accountService.getBalance(2L);

        // Assert
        // For Liability, balance = credits (5000) - debits (0) = 5000
        assertEquals(5000L, response.getBalanceMinor());
    }
}
