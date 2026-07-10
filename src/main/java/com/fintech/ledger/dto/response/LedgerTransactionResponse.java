package com.fintech.ledger.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LedgerTransactionResponse {
    private Long journalEntryId;
    private String referenceId;
    private String message;
}