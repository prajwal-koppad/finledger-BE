package com.fintech.ledger.controller;

import com.fintech.ledger.dto.request.CreateLedgerTransactionRequest;
import com.fintech.ledger.dto.response.LedgerTransactionResponse;
import com.fintech.ledger.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @PostMapping("/transactions")
    public LedgerTransactionResponse recordTransaction(@Valid @RequestBody CreateLedgerTransactionRequest request) {
        return ledgerService.recordTransaction(request);
    }
}