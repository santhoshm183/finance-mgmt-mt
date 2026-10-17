ALTER TABLE finance_mgmt.customers ADD COLUMN normalized_name VARCHAR(150);

UPDATE finance_mgmt.customers
SET normalized_name = LOWER(TRIM(name));

ALTER TABLE finance_mgmt.customers ALTER COLUMN normalized_name SET NOT NULL;

CREATE UNIQUE INDEX idx_customers_name_normalized
    ON finance_mgmt.customers (normalized_name);
