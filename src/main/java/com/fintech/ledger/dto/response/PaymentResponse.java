package com.fintech.ledger.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private Long paymentId;
    private String paymentReference;
    private Long invoiceId;
    private Long amountMinor;
    private String status;
    private Long invoicePaidAmountMinor;
    private String invoiceStatus;
    private boolean duplicate;
}