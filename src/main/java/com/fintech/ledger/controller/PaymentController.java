package com.fintech.ledger.controller;

import com.fintech.ledger.dto.request.ApplyPaymentRequest;
import com.fintech.ledger.dto.response.PaymentResponse;
import com.fintech.ledger.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices/{invoiceId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public PaymentResponse applyPayment(@PathVariable Long invoiceId,
                                        @Valid @RequestBody ApplyPaymentRequest request) {
        return paymentService.applyPayment(invoiceId, request);
    }
}