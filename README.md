# Ledger & Invoice Service - Fintech Assignment

This repository contains the backend and frontend solutions for the Mini Payment Ledger & Invoice Service assignment.

## Tech Stack
* **Backend**: Java 17, Spring Boot, Spring Data JPA, H2 Database (In-Memory).
* **Frontend**: React, Vite, React Router, raw CSS (for clean and zero-dependency styles).
* **Build Tools**: Maven, npm.

## Core Implementation Features

### Part 1: Core Ledger
- Implemented **Double-Entry Accounting** through the `LedgerService`. Every financial transaction creates an immutable `JournalEntry` and matching `DEBIT` and `CREDIT` `LedgerEntry` records.
- **Natural Balances**: The system properly understands that "Debit" and "Credit" are directional depending on the account type. 
  - For **Assets and Expenses**: `Balance = Debits - Credits` (Debits increase the balance).
  - For **Liabilities, Equity, and Revenue**: `Balance = Credits - Debits` (Credits increase the balance).
- **Derived Balances**: Account balances are not stored as mutable numbers. They are dynamically calculated on the fly (`AccountService.getBalance`) by aggregating debits and credits from the transaction log based on the natural balance rule above.
- **Precision**: Used `Long` (`amountMinor`) representing cents across the board, strictly avoiding floating-point rounding errors.

### Part 2: Invoice Flow
- The `InvoiceService` allows creating invoices with nested Line Items and tracks state (`DRAFT`, `PARTIALLY_PAID`, `PAID`).
- **Payment Handling**: Implemented partial payments. Prevents overpayments by enforcing constraints against outstanding invoice balances.
- **Idempotency**: Implemented strict deduplication using `paymentReference`. If a payment webhook fires twice with the same reference, the API returns the existing payment instead of throwing an error or duplicating the ledger entries.

### Part 3: Edge-Case Challenge (Race Conditions)
- Selected: **Concurrent payments hitting the same invoice at the same time.**
- Handled this cleanly via a database Pessimistic Lock in JPA (`@Lock(LockModeType.PESSIMISTIC_WRITE)` / `findByIdForUpdate`). This ensures that if two identical payment webhooks hit the system at the exact same millisecond, the database serializes them, preventing invoice overpayment race conditions.

### Code Quality (SOLID Principles)
- Refactored `PaymentService` to delegate ledger side effects to the `LedgerService`. This adheres strictly to the **Single Responsibility Principle (SRP)**.
- Added comprehensive JUnit tests for the Ledger and Account core logic to prove the correctness of the double-entry system.
- Implemented tests to verify concurrency controls, ensuring payments strictly cannot exceed the outstanding invoice balance.
## API Usage Guide

### Ledger Accounts 
By default, the `data.sql` script seeds three accounts:
- **ID 1 (`CASH-01`)**: Corporate Cash (starting balance: `$10,000.00`)
- **ID 2 (`AR-01`)**: Accounts Receivable (starting balance: `$5,000.00`)
- **ID 3 (`EQ-01`)**: Initial Capital

You can create more accounts via `POST /api/accounts` and check their derived balances via `GET /api/accounts/{id}/balance`.

### Ledger Transactions
- **Endpoint**: `POST /api/ledger/transactions`
- **Usage**: Used to transfer money between two accounts. You must pass a `debitAccountId` and `creditAccountId` (e.g. from 1 to 2).

### Invoices & Payments
- **Create Invoice**: `POST /api/invoices` (Specify line items and due date).
- **Send Invoice**: `POST /api/invoices/{id}/send` (Moves status from DRAFT to SENT).
- **Apply Payment**: `POST /api/invoices/{id}/payments`. 
  - *Note:* The system automatically debits the Cash Account (ID 1) and credits the Accounts Receivable Account (ID 2). Ensure you provide a unique `paymentReference` to test the idempotency feature!

## How to Run

### Backend (`ledger-be`)
1. Ensure Java 17 and Maven are installed.
2. Navigate to the `ledger-be` directory:
   ```bash
   cd ledger-be
   mvn clean install
   mvn spring-boot:run
   ```
3. The API will start at `http://localhost:8080`.
4. (Optional) Run tests with `mvn test`.

### Frontend (`ledger-ui`)
1. Ensure Node.js is installed.
2. Navigate to the `ledger-ui` directory:
   ```bash
   cd ledger-ui
   npm install
   npm run dev
   ```
3. The frontend proxy will automatically map `/api` to `localhost:8080` to bypass CORS issues.

## What I'd Do Differently With More Time
1. **Event-Driven Architecture (Kafka):** Instead of synchronously saving Ledger Entries within the `PaymentService` transaction, I would publish an `InvoicePaid` event to a Kafka topic. A separate consumer would process the event and write the double-entry records. This decouples the billing domain from the accounting domain.
2. **GraphQL:** I used REST for simplicity given the time constraints, but I would migrate the read paths (e.g., fetching Account Balances and Invoice states) to GraphQL to match your team's stack.

## Shortcuts Taken
- **Database:** Used an H2 in-memory database to allow you to run the project effortlessly without spinning up PostgreSQL or MySQL containers.
- **Hardcoded Default Accounts:** In `PaymentService`, I hardcoded a `DEFAULT_CASH_ACCOUNT_ID` and `DEFAULT_RECEIVABLE_ACCOUNT_ID` for simplicity in the API payload, rather than deriving them dynamically from tenant configs.
- **Auth:** Omitted Authentication (JWT) and Authorization to focus purely on the financial state machine and ledger accuracy.
