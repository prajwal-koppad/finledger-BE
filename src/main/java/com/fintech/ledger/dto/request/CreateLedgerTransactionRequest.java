package com.fintech.ledger.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLedgerTransactionRequest {

    @NotBlank
    private String referenceId;

    @NotBlank
    private String description;

    @NotNull
    private Long debitAccountId;

    @NotNull
    private Long creditAccountId;

    @NotNull
    @Min(1)
    private Long amountMinor;
}