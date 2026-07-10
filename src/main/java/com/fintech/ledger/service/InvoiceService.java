package com.fintech.ledger.service;

import com.fintech.ledger.dto.request.CreateInvoiceRequest;
import com.fintech.ledger.dto.response.InvoiceResponse;
import com.fintech.ledger.entities.Account;
import com.fintech.ledger.entities.Invoice;
import com.fintech.ledger.entities.InvoiceLineItem;
import com.fintech.ledger.enums.InvoiceStatus;
import com.fintech.ledger.exception.NotFoundException;
import com.fintech.ledger.repository.AccountRepository;
import com.fintech.ledger.repository.InvoiceLineItemRepository;
import com.fintech.ledger.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        Account customerAccount = null;
        if (request.getCustomerAccountId() != null) {
            customerAccount = accountRepository.findById(request.getCustomerAccountId())
                    .orElseThrow(() -> new NotFoundException("Customer account not found"));
        }

        long totalAmountMinor = request.getLineItems().stream()
                .mapToLong(item -> item.getQuantity() * item.getUnitPriceMinor())
                .sum();

        Invoice invoice = invoiceRepository.save(
                Invoice.builder()
                        .invoiceNumber(request.getInvoiceNumber())
                        .customerAccount(customerAccount)
                        .currency(request.getCurrency())
                        .totalAmountMinor(totalAmountMinor)
                        .paidAmountMinor(0L)
                        .dueDate(request.getDueDate())
                        .status(InvoiceStatus.DRAFT)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        for (CreateInvoiceRequest.InvoiceLineItemRequest item : request.getLineItems()) {
            long lineTotal = item.getQuantity() * item.getUnitPriceMinor();
            invoiceLineItemRepository.save(
                    InvoiceLineItem.builder()
                            .invoice(invoice)
                            .description(item.getDescription())
                            .quantity(item.getQuantity())
                            .unitPriceMinor(item.getUnitPriceMinor())
                            .lineTotalMinor(lineTotal)
                            .build()
            );
        }

        return getInvoice(invoice.getId());
    }

    @Transactional
    public InvoiceResponse sendInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));

        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        return getInvoice(invoiceId);
    }

    public InvoiceResponse getInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));

        List<InvoiceLineItem> items = invoiceLineItemRepository.findByInvoiceId(invoiceId);

        InvoiceStatus effectiveStatus = deriveEffectiveStatus(invoice);

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .currency(invoice.getCurrency())
                .totalAmountMinor(invoice.getTotalAmountMinor())
                .paidAmountMinor(invoice.getPaidAmountMinor())
                .outstandingAmountMinor(invoice.getTotalAmountMinor() - invoice.getPaidAmountMinor())
                .status(effectiveStatus.name())
                .dueDate(invoice.getDueDate())
                .lineItems(items.stream().map(item ->
                        InvoiceResponse.InvoiceLineItemDto.builder()
                                .description(item.getDescription())
                                .quantity(item.getQuantity())
                                .unitPriceMinor(item.getUnitPriceMinor())
                                .lineTotalMinor(item.getLineTotalMinor())
                                .build()
                ).toList())
                .build();
    }

    private InvoiceStatus deriveEffectiveStatus(Invoice invoice) {
        if (invoice.getPaidAmountMinor() >= invoice.getTotalAmountMinor()) {
            return InvoiceStatus.PAID;
        }

        if (invoice.getPaidAmountMinor() > 0) {
            return InvoiceStatus.PARTIALLY_PAID;
        }

        if ((invoice.getStatus() == InvoiceStatus.SENT || invoice.getStatus() == InvoiceStatus.OVERDUE)
                && LocalDate.now().isAfter(invoice.getDueDate())) {
            return InvoiceStatus.OVERDUE;
        }

        return invoice.getStatus();
    }
}