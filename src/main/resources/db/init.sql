-- HameedMoneyCLI (HMC) Database Initialization Script
CREATE SCHEMA IF NOT EXISTS app_data;
SET search_path to app_data, public;

-- Cleanup (Optional: Remove if running on an existing production DB)
DROP TABLE IF EXISTS market_quote;
DROP TABLE IF EXISTS ingestion_rule;
DROP TABLE IF EXISTS transaction;
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS asset;
DROP TYPE IF EXISTS account_type;
DROP TYPE IF EXISTS transaction_type;
DROP TYPE IF EXISTS asset_category;

CREATE TYPE account_type AS ENUM ('ASSET', 'LIABILITY', 'INCOME', 'EXPENSE');
CREATE TYPE transaction_type AS ENUM ('CARD_PURCHASE', 'BANK_TRANSFER', 'STOCK_PURCHASE');
CREATE TYPE asset_category AS ENUM ('STOCK', 'CASH', 'CRYPTO', 'COMMODITY');

-- 1. THE ASSET REGISTRY
-- Defines "What" can be owned.
CREATE TABLE asset
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)       NOT NULL, -- e.g., "CIB Stock", "Egyptian Pound"
    symbol      VARCHAR(20) UNIQUE NOT NULL, -- e.g., "COMI.CA", "EGP", "USD"
    category    asset_category NOT NULL,
    is_tradable BOOLEAN DEFAULT TRUE         -- False for static assets like "Physical Gold"
);

-- 2. THE ACCOUNT HIERARCHY
-- Defines "Where" the assets live. Implements a Parent-Child tree.
CREATE TABLE account
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    master_type account_type NOT NULL, -- 'ASSET', 'LIABILITY', 'INCOME', 'EXPENSE'
    parent_id   BIGINT,                -- Self-reference for hierarchy
    asset_id    BIGINT       NOT NULL, -- The "Currency" of this account
    running_balance DECIMAL(19, 4) DEFAULT 0, -- Cached balance for quick access
    is_internal BOOLEAN DEFAULT TRUE,  -- TRUE: Your wallet/bank. FALSE: Categories/Vendors.
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NULL,

    CONSTRAINT fk_account_parent FOREIGN KEY (parent_id) REFERENCES account (id),
    CONSTRAINT fk_account_asset FOREIGN KEY (asset_id) REFERENCES asset (id)
);

-- 3. THE TRANSACTION ENGINE
-- The "Source/Destination" model. Handles multi-currency bridges and fees.
CREATE TABLE transaction
(
    id               BIGSERIAL PRIMARY KEY,
    description      TEXT   NOT NULL,
    type             transaction_type NOT NULL,

    -- Inflow/Outflow mechanics
    from_account_id  BIGINT         NOT NULL,
    from_amount      DECIMAL(19, 4) NOT NULL,  -- Amount leaving source
    to_account_id    BIGINT         NOT NULL,
    to_amount        DECIMAL(19, 4) NOT NULL,  -- Amount entering destination

    fee_amount       DECIMAL(19, 4) DEFAULT 0, -- Commissions (Thndr/Bank fees)

    -- System Metadata
    external_ref_id  VARCHAR(255) UNIQUE,      -- Idempotency key (Hash of CSV row)
    source_system    VARCHAR(50),              -- 'HSBC', 'THNDR', 'MANUAL'
    metadata         JSONB,
    created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_from_account FOREIGN KEY (from_account_id) REFERENCES account (id),
    CONSTRAINT fk_to_account FOREIGN KEY (to_account_id) REFERENCES account (id)
);

-- 4. THE INTELLIGENCE LAYER (Ingestion Rules)
-- Automates the "Description -> Category" mapping.
CREATE TABLE ingestion_rule
(
    id                BIGSERIAL PRIMARY KEY,
    match_pattern     VARCHAR(255) NOT NULL, -- Regex or Keyword (e.g., "UBER.*")
    target_account_id BIGINT       NOT NULL, -- Target Category Account
    priority          INT DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rule_target FOREIGN KEY (target_account_id) REFERENCES account (id)
);

-- 5. THE ORACLE (Market Quotes)
-- Stores historical prices for Net Worth recalculation.
CREATE TABLE market_quote
(
    id BIGSERIAL PRIMARY KEY,
    base_asset_id BIGINT NOT NULL, -- The asset being priced (e.g., AAPL)
    quote_asset_id BIGINT NOT NULL, -- The "Price Tag" asset (e.g., USD)
    quote_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    price DECIMAL(19, 8) NOT NULL, -- Using high precision for crypto/FX
    CONSTRAINT fk_base_asset FOREIGN KEY (base_asset_id) REFERENCES asset(id),
    CONSTRAINT fk_quote_asset FOREIGN KEY (quote_asset_id) REFERENCES asset(id),
    CONSTRAINT unique_daily_quote UNIQUE (base_asset_id, quote_asset_id, quote_date)
);

-- Optional: Initial Seed Data for Base Currencies
INSERT INTO asset (name, symbol, category, is_tradable)
VALUES ('Egyptian Pound', 'EGP', 'CASH', FALSE),
       ('US Dollar', 'USD', 'CASH', FALSE);