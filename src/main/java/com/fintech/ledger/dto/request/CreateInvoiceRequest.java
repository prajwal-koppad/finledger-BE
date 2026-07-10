package com.fintech.ledger.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateInvoiceRequest {

    @NotBlank
    private String invoiceNumber;

    private Long customerAccountId;

    @NotBlank
    private String currency;

    @NotNull
    @FutureOrPresent
    private LocalDate dueDate;

    @NotEmpty
    @Valid
    private List<InvoiceLineItemRequest> lineItems;

    @Data
    public static class InvoiceLineItemRequest {

        @NotBlank
        private String description;

        @NotNull
        @Min(1)
        private Integer quantity;

        @NotNull
        @Min(1)
        private Long unitPriceMinor;
    }
}