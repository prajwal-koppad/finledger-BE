package com.fintech.ledger.controller;

import com.fintech.ledger.dto.request.CreateAccountRequest;
import com.fintech.ledger.dto.response.AccountBalanceResponse;
import com.fintech.ledger.entities.Account;
import com.fintech.ledger.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public Account createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    @GetMapping("/{accountId}/balance")
    public AccountBalanceResponse getBalance(@PathVariable Long accountId) {
        return accountService.getBalance(accountId);
    }
}