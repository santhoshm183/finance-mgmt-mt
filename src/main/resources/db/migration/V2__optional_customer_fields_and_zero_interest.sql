ALTER TABLE finance_mgmt.customers ALTER COLUMN mobile_number DROP NOT NULL;
ALTER TABLE finance_mgmt.customers ALTER COLUMN address DROP NOT NULL;

ALTER TABLE finance_mgmt.financial_transactions
    DROP CONSTRAINT IF EXISTS financial_transactions_amount_check;
ALTER TABLE finance_mgmt.financial_transactions
    DROP CONSTRAINT IF EXISTS CONSTRAINT_DF0;
ALTER TABLE finance_mgmt.financial_transactions
    ADD CONSTRAINT financial_transactions_amount_nonnegative CHECK (amount >= 0);

ALTER TABLE finance_mgmt.loans DROP CONSTRAINT IF EXISTS loans_interest_rate_check;
ALTER TABLE finance_mgmt.loans DROP CONSTRAINT IF EXISTS CONSTRAINT_453E;
ALTER TABLE finance_mgmt.loans
    ADD CONSTRAINT loans_interest_rate_nonnegative CHECK (interest_rate >= 0);
