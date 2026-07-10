-- Insert default Cash Account (ID 1)
INSERT IGNORE INTO accounts (id, account_code, name, type, currency, created_at) 
VALUES (1, 'CASH-01', 'Corporate Cash', 'ASSET', 'USD', CURRENT_TIMESTAMP);

-- Insert default Receivable Account (ID 2)
INSERT IGNORE INTO accounts (id, account_code, name, type, currency, created_at) 
VALUES (2, 'AR-01', 'Accounts Receivable', 'ASSET', 'USD', CURRENT_TIMESTAMP);

-- You can add more seed data here if required for testing

-- Insert default Equity Account (ID 3)
INSERT IGNORE INTO accounts (id, account_code, name, type, currency, created_at) 
VALUES (3, 'EQ-01', 'Initial Capital', 'EQUITY', 'USD', CURRENT_TIMESTAMP);

-- Insert Initial Seed Journal Entry
INSERT IGNORE INTO journal_entries (id, reference_id, reference_type, description, created_at)
VALUES (1, 'SEED-001', 'TRANSFER', 'Initial seed funding', CURRENT_TIMESTAMP);

-- Debit Cash (ID 1) by $10,000.00 (1,000,000 minor units)
INSERT IGNORE INTO ledger_entries (id, journal_entry_id, account_id, entry_type, amount_minor, created_at)
VALUES (1, 1, 1, 'DEBIT', 1000000, CURRENT_TIMESTAMP);

-- Debit Accounts Receivable (ID 2) by $5,000.00 (500,000 minor units)
INSERT IGNORE INTO ledger_entries (id, journal_entry_id, account_id, entry_type, amount_minor, created_at)
VALUES (2, 1, 2, 'DEBIT', 500000, CURRENT_TIMESTAMP);

-- Credit Equity (ID 3) by $15,000.00 (1,500,000 minor units) to balance the ledger
INSERT IGNORE INTO ledger_entries (id, journal_entry_id, account_id, entry_type, amount_minor, created_at)
VALUES (3, 1, 3, 'CREDIT', 1500000, CURRENT_TIMESTAMP);
