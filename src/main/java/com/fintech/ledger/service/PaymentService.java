package com.fintech.ledger.service;

import com.fintech.ledger.dto.request.ApplyPaymentRequest;
import com.fintech.ledger.dto.response.PaymentResponse;
import com.fintech.ledger.entities.*;
import com.fintech.ledger.enums.InvoiceStatus;
import com.fintech.ledger.enums.PaymentStatus;
import com.fintech.ledger.enums.ReferenceType;
import com.fintech.ledger.exception.NotFoundException;
import com.fintech.ledger.exception.OverpaymentException;
import com.fintech.ledger.exception.ValidationException;
import com.fintech.ledger.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.fintech.ledger.enums.InvoiceStatus.PAID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final AccountRepository accountRepository;
    private final LedgerService ledgerService;
    private final AccountService accountService;

    private static final Long DEFAULT_CASH_ACCOUNT_ID = 1L;
    private static final Long DEFAULT_RECEIVABLE_ACCOUNT_ID = 2L;

    @Transactional
    public PaymentResponse applyPayment(Long invoiceId, ApplyPaymentRequest request) {

        // If a webhook fires twice for the same payment reference, we just return the existing data 
        // instead of throwing an error or double-charging.
        if (paymentRepository.existsByPaymentReference(request.getPaymentReference())) {
            Payment existing = paymentRepository.findByPaymentReference(request.getPaymentReference())
                    .orElseThrow();

            Invoice existingInvoice = existing.getInvoice();
            return PaymentResponse.builder()
                    .paymentId(existing.getId())
                    .paymentReference(existing.getPaymentReference())
                    .invoiceId(existingInvoice.getId())
                    .amountMinor(existing.getAmountMinor())
                    .status(existing.getStatus().name())
                    .invoicePaidAmountMinor(existingInvoice.getPaidAmountMinor())
                    .invoiceStatus(existingInvoice.getStatus().name())
                    .duplicate(true)
                    .build();
        }

        // Edge-case Challenge: Handled race conditions by using a database pessimistic lock.
        // This ensures if two identical payment threads hit this line, one will wait for the lock to release.
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));

        if (PAID.equals(invoice.getStatus())) {
            throw new OverpaymentException("Invoice is fully paid");
        }

        long outstanding = invoice.getTotalAmountMinor() - invoice.getPaidAmountMinor();
        if (request.getAmountMinor() > outstanding) {
            throw new OverpaymentException("Payment exceeds outstanding invoice amount");
        }

        Account cashAccount = accountRepository.findById(
                        request.getDebitAccountId() != null ? request.getDebitAccountId() : DEFAULT_CASH_ACCOUNT_ID)
                .orElseThrow(() -> new NotFoundException("Cash account not found"));

        Account receivableAccount = accountRepository.findById(DEFAULT_RECEIVABLE_ACCOUNT_ID)
                .orElseThrow(() -> new NotFoundException("Receivable account not found"));


        if (!cashAccount.getCurrency().equalsIgnoreCase(invoice.getCurrency()) ||
                !receivableAccount.getCurrency().equalsIgnoreCase(invoice.getCurrency())) {
            throw new ValidationException("Payment account currency must match invoice currency");
        }

        Payment payment = paymentRepository.save(
                Payment.builder()
                        .paymentReference(request.getPaymentReference())
                        .invoice(invoice)
                        .amountMinor(request.getAmountMinor())
                        .status(PaymentStatus.APPLIED)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // Delegating to LedgerService to strictly adhere to SRP. 
        // PaymentService shouldn't know how to create Journal and Ledger entries directly.
        ledgerService.recordSystemTransaction(
                cashAccount,
                receivableAccount,
                request.getAmountMinor(),
                request.getPaymentReference(),
                "Payment applied to invoice " + invoice.getInvoiceNumber(),
                ReferenceType.PAYMENT
        );

        long newPaidAmount = invoice.getPaidAmountMinor() + request.getAmountMinor();
        invoice.setPaidAmountMinor(newPaidAmount);

        if (newPaidAmount == invoice.getTotalAmountMinor()) {
            invoice.setStatus(PAID);
        } else if (newPaidAmount > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .invoiceId(invoice.getId())
                .amountMinor(payment.getAmountMinor())
                .status(payment.getStatus().name())
                .invoicePaidAmountMinor(invoice.getPaidAmountMinor())
                .invoiceStatus(invoice.getStatus().name())
                .duplicate(false)
                .build();
    }
}