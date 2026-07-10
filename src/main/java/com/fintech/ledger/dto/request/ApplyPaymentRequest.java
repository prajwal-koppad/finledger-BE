package com.fintech.ledger.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplyPaymentRequest {

    @NotBlank
    private String paymentReference;

    @NotNull
    @Min(1)
    private Long amountMinor;

    // Optional if you want to specify cash/bank account used
    private Long debitAccountId;
}