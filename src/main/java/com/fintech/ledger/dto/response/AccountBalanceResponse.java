package com.fintech.ledger.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountBalanceResponse {
    private Long accountId;
    private String accountCode;
    private String currency;
    private Long balanceMinor;
}