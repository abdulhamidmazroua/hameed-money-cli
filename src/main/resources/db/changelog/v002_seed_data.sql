-- Seed currencies (idempotent)
INSERT INTO asset (name, symbol, category, is_tradable)
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
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Cash', 'ASSET', NULL, NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Cash' AND parent_id IS NULL);

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Securities', 'ASSET', NULL, NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Securities' AND parent_id IS NULL);

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Fixed', 'ASSET', NULL, NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Fixed' AND parent_id IS NULL);

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Debt', 'LIABILITY', NULL, NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Debt' AND parent_id IS NULL);

-- Seed cash accounts (idempotent)
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'HSBC Current Account', 'ASSET', (SELECT id FROM account WHERE name = 'Cash'), (SELECT id FROM asset WHERE symbol = 'EGP'), TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'HSBC Current Account');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Misr Current Account', 'ASSET', (SELECT id FROM account WHERE name = 'Cash'), (SELECT id FROM asset WHERE symbol = 'EGP'), TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Misr Current Account');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Thndr Investment Account', 'ASSET', (SELECT id FROM account WHERE name = 'Cash'), (SELECT id FROM asset WHERE symbol = 'EGP'), TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Thndr Investment Account');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Loan Account', 'LIABILITY', (SELECT id FROM account WHERE name = 'Cash'), (SELECT id FROM asset WHERE symbol = 'EGP'), TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Loan Account');

-- Seed portfolio folders (idempotent)
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Thndr Portfolio', 'ASSET', (SELECT id FROM account WHERE name = 'Securities'), NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Thndr Portfolio');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Etoro Portfolio', 'ASSET', (SELECT id FROM account WHERE name = 'Securities'), NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Etoro Portfolio');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Property', 'ASSET', (SELECT id FROM account WHERE name = 'Fixed'), NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Property');

-- Seed income accounts (idempotent)
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Basic Salary', 'INCOME', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Basic Salary');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Bonus', 'INCOME', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Bonus');

-- Seed expense accounts (idempotent)
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Food', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Food');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Groceries', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Groceries');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Subscriptions', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Subscriptions');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Other', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Other');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'PocketMoney', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'PocketMoney');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'MobileRecharge', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'MobileRecharge');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Family', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Family');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Charity', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Charity');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Lending', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Lending');

-- Seed SYSTEM account trio for EGP (idempotent)
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'Opening EGP Balance', 'SYSTEM', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'Opening EGP Balance');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP Balance Increase Adjustment', 'SYSTEM', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP Balance Increase Adjustment');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP Balance Decrease Adjustment', 'SYSTEM', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP Balance Decrease Adjustment');

-- Seed source systems (idempotent — code is UNIQUE)
INSERT INTO source_system (name, code, anchored_account_id)
SELECT 'HSBC Egypt App', 'HSBC_APP', (SELECT id FROM account WHERE name = 'HSBC Current Account')
ON CONFLICT (code) DO NOTHING;

INSERT INTO source_system (name, code, anchored_account_id)
SELECT 'Banque Misr App', 'BANQUE_MISR_APP', (SELECT id FROM account WHERE name = 'Misr Current Account')
ON CONFLICT (code) DO NOTHING;

INSERT INTO source_system (name, code, anchored_account_id)
SELECT 'Thndr App', 'THNDR_APP', (SELECT id FROM account WHERE name = 'Thndr Investment Account')
ON CONFLICT (code) DO NOTHING;

INSERT INTO source_system (name, code, anchored_account_id)
SELECT 'Manual entry', 'MANUAL_ENTRY', (SELECT id FROM account WHERE name = 'HSBC Current Account')
ON CONFLICT (code) DO NOTHING;

-- Seed ingestion rules (idempotent)
INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*Thndr.*', (SELECT id FROM account WHERE name = 'Thndr Investment Account'), 200
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*Thndr.*');

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*(Life Makers|Zakat|Sadakat|Bait El).*', (SELECT id FROM account WHERE name = 'Charity'), 190
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*(Life Makers|Zakat|Sadakat|Bait El).*');

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*(Mobile Recharge|Land Line|Home Internet|Purchase from).*', (SELECT id FROM account WHERE name = 'MobileRecharge'), 180
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*(Mobile Recharge|Land Line|Home Internet|Purchase from).*');

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*CARD TRANSACTION.*ATM.*', (SELECT id FROM account WHERE name = 'PocketMoney'), 175
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*CARD TRANSACTION.*ATM.*');

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*CARD TRANSACTION.*', (SELECT id FROM account WHERE name = 'Food'), 50
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*CARD TRANSACTION.*');

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*Instant Transfer from.*', (SELECT id FROM account WHERE name = 'Basic Salary'), 40
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*Instant Transfer from.*');

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*', (SELECT id FROM account WHERE name = 'Other'), -1000
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*');
