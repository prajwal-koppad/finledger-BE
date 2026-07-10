package com.fintech.ledger.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private String currency;
    private Long totalAmountMinor;
    private Long paidAmountMinor;
    private Long outstandingAmountMinor;
    private String status;
    private LocalDate dueDate;
    private List<InvoiceLineItemDto> lineItems;

    @Data
    @Builder
    public static class InvoiceLineItemDto {
        private String description;
        private Integer quantity;
        private Long unitPriceMinor;
        private Long lineTotalMinor;
    }
}