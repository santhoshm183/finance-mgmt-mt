INSERT INTO finance_mgmt.cash_flow (id, investment_amount, financial_balance, profit_amount, collection_amount)
SELECT 1, 0.00, 0.00, 0.00, 0.00
WHERE NOT EXISTS (
    SELECT 1
    FROM finance_mgmt.cash_flow
    WHERE id = 1
);
