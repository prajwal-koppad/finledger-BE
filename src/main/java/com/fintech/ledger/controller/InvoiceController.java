package com.fintech.ledger.controller;

import com.fintech.ledger.dto.request.CreateInvoiceRequest;
import com.fintech.ledger.dto.response.InvoiceResponse;
import com.fintech.ledger.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public InvoiceResponse createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        return invoiceService.createInvoice(request);
    }

    @PostMapping("/{invoiceId}/send")
    public InvoiceResponse sendInvoice(@PathVariable Long invoiceId) {
        return invoiceService.sendInvoice(invoiceId);
    }

    @GetMapping("/{invoiceId}")
    public InvoiceResponse getInvoice(@PathVariable Long invoiceId) {
        return invoiceService.getInvoice(invoiceId);
    }
}