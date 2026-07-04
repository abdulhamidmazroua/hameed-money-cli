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
SELECT 'EGP:HSBC Current Account', 'ASSET', (SELECT id FROM account WHERE name = 'Cash'), (SELECT id FROM asset WHERE symbol = 'EGP'), TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:HSBC Current Account');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Misr Current Account', 'ASSET', (SELECT id FROM account WHERE name = 'Cash'), (SELECT id FROM asset WHERE symbol = 'EGP'), TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Misr Current Account');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Thndr Investment Account', 'ASSET', (SELECT id FROM account WHERE name = 'Cash'), (SELECT id FROM asset WHERE symbol = 'EGP'), TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Thndr Investment Account');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Loan Account', 'LIABILITY', (SELECT id FROM account WHERE name = 'Cash'), (SELECT id FROM asset WHERE symbol = 'EGP'), TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Loan Account');

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
SELECT 'EGP:Basic Salary', 'INCOME', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Basic Salary');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Bonus', 'INCOME', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Bonus');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:Basic Salary', 'INCOME', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:Basic Salary');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:Bonus', 'INCOME', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:Bonus');

-- Seed expense accounts (idempotent)
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Food', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Food');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Groceries', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Groceries');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Subscriptions', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Subscriptions');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Other', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Other');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:PocketMoney', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:PocketMoney');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:MobileRecharge', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:MobileRecharge');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Family', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Family');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Charity', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Charity');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Lending', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Lending');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:Food', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:Food');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:Groceries', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:Groceries');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:Subscriptions', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:Subscriptions');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:Other', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:Other');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:PocketMoney', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:PocketMoney');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:MobileRecharge', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:MobileRecharge');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:Family', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:Family');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:Charity', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:Charity');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:Lending', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:Lending');

-- Seed SYSTEM account trios (idempotent)
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Opening Balance', 'SYSTEM', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Opening Balance');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Balance Increase Adjustment', 'SYSTEM', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Balance Increase Adjustment');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'EGP:Balance Decrease Adjustment', 'SYSTEM', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'EGP:Balance Decrease Adjustment');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:Opening Balance', 'SYSTEM', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:Opening Balance');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:Balance Increase Adjustment', 'SYSTEM', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:Balance Increase Adjustment');

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
SELECT 'AED:Balance Decrease Adjustment', 'SYSTEM', NULL, (SELECT id FROM asset WHERE symbol = 'AED'), FALSE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE name = 'AED:Balance Decrease Adjustment');

-- Seed source systems (idempotent — code is UNIQUE)
INSERT INTO source_system (name, code, anchored_account_id)
SELECT 'HSBC Egypt App', 'HSBC_APP', (SELECT id FROM account WHERE name = 'EGP:HSBC Current Account')
ON CONFLICT (code) DO NOTHING;

INSERT INTO source_system (name, code, anchored_account_id)
SELECT 'Banque Misr App', 'BANQUE_MISR_APP', (SELECT id FROM account WHERE name = 'EGP:Misr Current Account')
ON CONFLICT (code) DO NOTHING;

INSERT INTO source_system (name, code, anchored_account_id)
SELECT 'Thndr App', 'THNDR_APP', (SELECT id FROM account WHERE name = 'EGP:Thndr Investment Account')
ON CONFLICT (code) DO NOTHING;

INSERT INTO source_system (name, code, anchored_account_id)
SELECT 'Manual entry', 'MANUAL_ENTRY', (SELECT id FROM account WHERE name = 'EGP:HSBC Current Account')
ON CONFLICT (code) DO NOTHING;

-- Seed ingestion rules (idempotent)
INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*Thndr.*', (SELECT id FROM account WHERE name = 'EGP:Thndr Investment Account'), 200
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*Thndr.*');

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*(Life Makers|Zakat|Sadakat|Bait El).*', (SELECT id FROM account WHERE name = 'EGP:Charity'), 190
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*(Life Makers|Zakat|Sadakat|Bait El).*');

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*(Mobile Recharge|Land Line|Home Internet|Purchase from).*', (SELECT id FROM account WHERE name = 'EGP:MobileRecharge'), 180
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*(Mobile Recharge|Land Line|Home Internet|Purchase from).*');

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*CARD TRANSACTION.*ATM.*', (SELECT id FROM account WHERE name = 'EGP:PocketMoney'), 175
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*CARD TRANSACTION.*ATM.*');

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*CARD TRANSACTION.*', (SELECT id FROM account WHERE name = 'EGP:Food'), 50
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*CARD TRANSACTION.*');

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*Instant Transfer from.*', (SELECT id FROM account WHERE name = 'EGP:Basic Salary'), 40
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*Instant Transfer from.*');

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
SELECT '(?i).*', (SELECT id FROM account WHERE name = 'EGP:Other'), -1000
WHERE NOT EXISTS (SELECT 1 FROM ingestion_rule WHERE match_pattern = '(?i).*');
