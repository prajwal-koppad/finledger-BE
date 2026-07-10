package com.fintech.ledger.service;

import com.fintech.ledger.dto.request.ApplyPaymentRequest;
import com.fintech.ledger.entities.Account;
import com.fintech.ledger.entities.Invoice;
import com.fintech.ledger.enums.InvoiceStatus;
import com.fintech.ledger.exception.OverpaymentException;
import com.fintech.ledger.repository.AccountRepository;
import com.fintech.ledger.repository.InvoiceRepository;
import com.fintech.ledger.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private LedgerService ledgerService;
    @Mock
    private AccountService accountService;

    @InjectMocks
    private PaymentService paymentService;

    private Invoice mockInvoice;
    private ApplyPaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        mockInvoice = Invoice.builder()
                .id(1L)
                .invoiceNumber("INV-001")
                .totalAmountMinor(10000L) // $100.00
                .paidAmountMinor(0L)
                .currency("USD")
                .status(InvoiceStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRequest = new ApplyPaymentRequest();
        paymentRequest.setPaymentReference("PAY-123");
        paymentRequest.setAmountMinor(15000L); // $150.00 - Deliberately more than outstanding
    }

    @Test
    void applyPayment_VerifiesPessimisticLock_AndThrowsOverpaymentException() {
        // Arrange: Mock the idempotency check
        when(paymentRepository.existsByPaymentReference("PAY-123")).thenReturn(false);
        
        // Arrange: Mock the lock method! This verifies we are using the pessimistic lock method
        when(invoiceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockInvoice));

        // Act & Assert
        // We attempt to pay $150 towards a $100 invoice
        OverpaymentException ex = assertThrows(OverpaymentException.class, () -> {
            paymentService.applyPayment(1L, paymentRequest);
        });

        assertEquals("Payment exceeds outstanding invoice amount", ex.getMessage());

        // Verify that the exact method for pessimistic locking was called
        verify(invoiceRepository, times(1)).findByIdForUpdate(1L);
        // Verify we NEVER attempted to save the payment or ledger entries because it failed fast
        verify(paymentRepository, never()).save(any());
        verify(ledgerService, never()).recordSystemTransaction(any(), any(), anyLong(), anyString(), anyString(), any());
    }
}
