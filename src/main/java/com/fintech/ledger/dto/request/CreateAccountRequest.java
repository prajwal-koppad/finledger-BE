package com.fintech.ledger.dto.request;

import com.fintech.ledger.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotBlank
    private String accountCode;

    @NotBlank
    private String name;

    @NotNull
    private AccountType type;

    @NotBlank
    private String currency;
}