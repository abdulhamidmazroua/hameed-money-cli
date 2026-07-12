-- Create unique indexes for composite constraints (Hibernate's ddl-auto=update
-- can't add these via ALTER TABLE on SQLite — they must be inline in CREATE TABLE)
CREATE UNIQUE INDEX IF NOT EXISTS idx_asset_symbol_category ON assets(symbol, category);
CREATE UNIQUE INDEX IF NOT EXISTS idx_market_quote_key ON market_quotes(base_asset_id, quote_asset_id, quote_date);

-- Seed currencies (idempotent)
INSERT INTO assets (name, symbol, category, is_tradable)
VALUES ('Egyptian Pound', 'EGP', 'CASH', FALSE),
       ('US Dollar', 'USD', 'CASH', FALSE),
       ('Euro', 'EUR', 'CASH', FALSE),
       ('British Pound', 'GBP', 'CASH', FALSE),
       ('Swiss Franc', 'CHF', 'CASH', FALSE),
       ('Canadian Dollar', 'CAD', 'CASH', FALSE),
       ('Australian Dollar', 'AUD', 'CASH', FALSE),
       ('Japanese Yen', 'JPY', 'CASH', FALSE),
       ('Chinese Yuan', 'CNY', 'CASH', FALSE),
       ('Saudi Riyal', 'SAR', 'CASH', FALSE),
       ('UAE Dirham', 'AED', 'CASH', FALSE),
       ('Kuwaiti Dinar', 'KWD', 'CASH', FALSE),
       ('Qatari Riyal', 'QAR', 'CASH', FALSE),
       ('Turkish Lira', 'TRY', 'CASH', FALSE)
ON CONFLICT (symbol, category) DO NOTHING;

-- Seed account folders (idempotent)
INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'Cash', 'ASSET', NULL, NULL, TRUE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'Cash' AND parent_id IS NULL);

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'Securities', 'ASSET', NULL, NULL, TRUE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'Securities' AND parent_id IS NULL);

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'Fixed', 'ASSET', NULL, NULL, TRUE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'Fixed' AND parent_id IS NULL);

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'Debt', 'LIABILITY', NULL, NULL, TRUE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'Debt' AND parent_id IS NULL);

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Loan Account', 'LIABILITY', (SELECT id FROM accounts WHERE name = 'Cash'), (SELECT id FROM assets WHERE symbol = 'EGP'), TRUE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Loan Account');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'Property', 'ASSET', (SELECT id FROM accounts WHERE name = 'Fixed'), NULL, TRUE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'Property');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Basic Salary', 'INCOME', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Basic Salary');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Bonus', 'INCOME', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Bonus');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:Basic Salary', 'INCOME', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:Basic Salary');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:Bonus', 'INCOME', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:Bonus');

-- Seed expense accounts (idempotent)
INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Food', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Food');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Groceries', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Groceries');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Subscriptions', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Subscriptions');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Other', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Other');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:PocketMoney', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:PocketMoney');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:MobileRecharge', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:MobileRecharge');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Family', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Family');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Charity', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Charity');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Lending', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Lending');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:Food', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:Food');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:Groceries', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:Groceries');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:Subscriptions', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:Subscriptions');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:Other', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:Other');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:PocketMoney', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:PocketMoney');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:MobileRecharge', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:MobileRecharge');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:Family', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:Family');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:Charity', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:Charity');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:Lending', 'EXPENSE', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:Lending');

-- Seed SYSTEM account trios (idempotent)
INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Opening Balance', 'SYSTEM', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Opening Balance');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Balance Increase Adjustment', 'SYSTEM', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Balance Increase Adjustment');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'EGP:Balance Decrease Adjustment', 'SYSTEM', NULL, (SELECT id FROM assets WHERE symbol = 'EGP'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'EGP:Balance Decrease Adjustment');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:Opening Balance', 'SYSTEM', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:Opening Balance');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:Balance Increase Adjustment', 'SYSTEM', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:Balance Increase Adjustment');

INSERT INTO accounts (name, master_type, parent_id, asset_id, is_internal, created_at)
SELECT 'AED:Balance Decrease Adjustment', 'SYSTEM', NULL, (SELECT id FROM assets WHERE symbol = 'AED'), FALSE, (CAST(strftime('%s','now') AS INTEGER) * 1000)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = 'AED:Balance Decrease Adjustment');
