package com.fintech.ledger.repository;

import com.fintech.ledger.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByPaymentReference(String paymentReference);
    Optional<Payment> findByPaymentReference(String paymentReference);
}