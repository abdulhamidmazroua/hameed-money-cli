-- HameedMoneyCLI (HMC) Database Initialization Script
-- Pure hierarchy: parent accounts have asset_id NULL; only leaf accounts reference an asset (currency / instrument).
-- Aligns with Wealth Reporting Tool (HMC)/Account Hierarchy.canvas
CREATE SCHEMA IF NOT EXISTS app_data;
SET search_path to app_data, public;

DROP TABLE IF EXISTS market_quote;
DROP TABLE IF EXISTS ingestion_rule;
DROP TABLE IF EXISTS transaction;
DROP TABLE IF EXISTS source_system;
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS asset;

-- Enum-like columns use VARCHAR; allowed values are defined in Java (AccountType, TransactionType, AssetCategory).

CREATE TABLE asset
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)       NOT NULL,
    symbol      VARCHAR(20)        NOT NULL,
    category    VARCHAR(32)        NOT NULL,
    is_tradable BOOLEAN NOT NULL DEFAULT TRUE,
    metadata    JSONB,
    UNIQUE (symbol, category)
);

CREATE TABLE account
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    master_type VARCHAR(32) NOT NULL,
    parent_id   BIGINT,
    asset_id    BIGINT,
    is_internal BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ DEFAULT NULL,

    CONSTRAINT fk_account_parent FOREIGN KEY (parent_id) REFERENCES account (id),
    CONSTRAINT fk_account_asset FOREIGN KEY (asset_id) REFERENCES asset (id)
);

CREATE TABLE source_system
(
    id                    BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(50) NOT NULL,
    code                  VARCHAR(20) NOT NULL UNIQUE,
    anchored_account_id   BIGINT NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_source_system_account FOREIGN KEY (anchored_account_id) REFERENCES account (id)
);

CREATE TABLE transaction
(
    id                 BIGSERIAL PRIMARY KEY,
    description        TEXT   NOT NULL,
    type               VARCHAR(32) NOT NULL,
    transaction_date   TIMESTAMPTZ NOT NULL,

    from_account_id    BIGINT         NOT NULL,
    from_amount        DECIMAL(19, 4) NOT NULL,
    to_account_id      BIGINT         NOT NULL,
    to_amount          DECIMAL(19, 4) NOT NULL,

    fee_amount         DECIMAL(19, 4) DEFAULT 0,

    external_ref_id    VARCHAR(255) UNIQUE,
    source_system_id   BIGINT        NOT NULL,
    is_system_adjustment BOOLEAN     NOT NULL DEFAULT FALSE,
    metadata           JSONB,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_from_account FOREIGN KEY (from_account_id) REFERENCES account (id),
    CONSTRAINT fk_to_account FOREIGN KEY (to_account_id) REFERENCES account (id),
    CONSTRAINT fk_tx_source_system FOREIGN KEY (source_system_id) REFERENCES source_system (id)
);

CREATE TABLE ingestion_rule
(
    id                BIGSERIAL PRIMARY KEY,
    match_pattern     VARCHAR(255) NOT NULL,
    target_account_id BIGINT       NOT NULL,
    priority          INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rule_target FOREIGN KEY (target_account_id) REFERENCES account (id)
);

CREATE TABLE market_quote
(
    id BIGSERIAL PRIMARY KEY,
    base_asset_id BIGINT NOT NULL,
    quote_asset_id BIGINT NOT NULL,
    quote_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    price DECIMAL(19, 8) NOT NULL,
    CONSTRAINT fk_base_asset FOREIGN KEY (base_asset_id) REFERENCES asset(id),
    CONSTRAINT fk_quote_asset FOREIGN KEY (quote_asset_id) REFERENCES asset(id),
    CONSTRAINT unique_daily_quote UNIQUE (base_asset_id, quote_asset_id, quote_date)
);

INSERT INTO asset (name, symbol, category, is_tradable)
--seed with common currencies;
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
       ('Turkish Lira', 'TRY', 'CASH', FALSE);

-- Organizational folders only where useful; master_type strings match Java AccountType, not separate account rows.
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
VALUES ('Cash', 'ASSET', NULL, NULL, TRUE),
       ('Securities', 'ASSET', NULL, NULL, TRUE),
       ('Fixed', 'ASSET', NULL, NULL, TRUE),
       ('Debt', 'LIABILITY', NULL, NULL, TRUE);

-- Cash bucket + leaves
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
VALUES ('HSBC Current Account', 'ASSET', (SELECT id FROM account WHERE name = 'Cash'), (SELECT id FROM asset WHERE symbol = 'EGP'), TRUE),
       ('Misr Current Account', 'ASSET', (SELECT id FROM account WHERE name = 'Cash'), (SELECT id FROM asset WHERE symbol = 'EGP'), TRUE),
       ('Thndr Investment Account', 'ASSET', (SELECT id FROM account WHERE name = 'Cash'), (SELECT id FROM asset WHERE symbol = 'EGP'), TRUE),
       ('Loan Account', 'LIABILITY', (SELECT id FROM account WHERE name = 'Cash'), (SELECT id FROM asset WHERE symbol = 'EGP'), TRUE);

-- Securities: portfolio folders + instrument leaves
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
VALUES ('Thndr Portfolio', 'ASSET', (SELECT id FROM account WHERE name = 'Securities'), NULL, TRUE),
       ('Etoro Portfolio', 'ASSET', (SELECT id FROM account WHERE name = 'Securities'), NULL, TRUE);

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
VALUES ('Property', 'ASSET', (SELECT id FROM account WHERE name = 'Fixed'), NULL, TRUE);

-- Income / expense leaves (no enum-named parent folders — parent_id NULL)
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
VALUES ('Basic Salary', 'INCOME', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE),
       ('Bonus', 'INCOME', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE);

INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
VALUES ('Food', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE),
       ('Groceries', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE),
       ('Subscriptions', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE),
       ('Other', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE),
       ('PocketMoney', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE),
       ('MobileRecharge', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE),
       ('Family', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE),
       ('Charity', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE),
       ('Lending', 'EXPENSE', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE);

-- System adjustments (see System Adjustments.md): SYSTEM master_type keeps them out of income/expense P&L; pair with is_system_adjustment on transaction
INSERT INTO account (name, master_type, parent_id, asset_id, is_internal)
VALUES ('Opening Balance', 'SYSTEM', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE),
       ('Balance Increase Adjustment', 'SYSTEM', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE),
       ('Balance Decrease Adjustment', 'SYSTEM', NULL, (SELECT id FROM asset WHERE symbol = 'EGP'), FALSE);

INSERT INTO source_system (name, code, anchored_account_id)
VALUES ('HSBC Egypt App', 'HSBC_APP', (SELECT id FROM account WHERE name = 'HSBC Current Account')),
       ('Banque Misr App', 'BANQUE_MISR_APP', (SELECT id FROM account WHERE name = 'Misr Current Account')),
       ('Thndr App', 'THNDR_APP', (SELECT id FROM account WHERE name = 'Thndr Investment Account')),
       ('Manual entry', 'MANUAL_ENTRY', (SELECT id FROM account WHERE name = 'HSBC Current Account'));

INSERT INTO ingestion_rule (match_pattern, target_account_id, priority)
VALUES ('(?i).*Thndr.*', (SELECT id FROM account WHERE name = 'Thndr Investment Account'), 200),
       ('(?i).*(Life Makers|Zakat|Sadakat|Bait El).*', (SELECT id FROM account WHERE name = 'Charity'), 190),
       ('(?i).*(Mobile Recharge|Land Line|Home Internet|Purchase from).*', (SELECT id FROM account WHERE name = 'MobileRecharge'), 180),
       ('(?i).*CARD TRANSACTION.*ATM.*', (SELECT id FROM account WHERE name = 'PocketMoney'), 175),
       ('(?i).*CARD TRANSACTION.*', (SELECT id FROM account WHERE name = 'Food'), 50),
       ('(?i).*Instant Transfer from.*', (SELECT id FROM account WHERE name = 'Basic Salary'), 40),
       ('(?i).*', (SELECT id FROM account WHERE name = 'Other'), -1000);
