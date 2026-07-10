package com.fintech.ledger.service;

import com.fintech.ledger.dto.request.CreateAccountRequest;
import com.fintech.ledger.dto.response.AccountBalanceResponse;
import com.fintech.ledger.entities.Account;
import com.fintech.ledger.exception.NotFoundException;
import com.fintech.ledger.exception.ValidationException;
import com.fintech.ledger.repository.AccountRepository;
import com.fintech.ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public Account createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByAccountCode(request.getAccountCode())) {
            throw new ValidationException("Account already exists");
        }

        Account account = Account.builder()
                .accountCode(request.getAccountCode())
                .name(request.getName())
                .type(request.getType())
                .currency(request.getCurrency())
                .createdAt(LocalDateTime.now())
                .build();

        return accountRepository.save(account);
    }

    public AccountBalanceResponse getBalance(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        Long debits = ledgerEntryRepository.sumDebitsByAccountId(accountId);
        Long credits = ledgerEntryRepository.sumCreditsByAccountId(accountId);

        // Instead of a flat (debits - credits), we check the account type.
        // Assets/Expenses increase with Debits. Liabilities/Equity/Revenue increase with Credits.
        long balance = switch (account.getType()) {
            case ASSET, EXPENSE -> debits - credits;
            case LIABILITY, EQUITY, REVENUE -> credits - debits;
        };

        return AccountBalanceResponse.builder()
                .accountId(account.getId())
                .accountCode(account.getAccountCode())
                .currency(account.getCurrency())
                .balanceMinor(balance)
                .build();
    }
}