-- HameedMoneyCLI schema — idempotent table creation
CREATE SCHEMA IF NOT EXISTS app_data;
SET search_path TO app_data, public;

CREATE TABLE IF NOT EXISTS asset
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)       NOT NULL,
    symbol      VARCHAR(20)        NOT NULL,
    category    VARCHAR(32)        NOT NULL,
    is_tradable BOOLEAN NOT NULL DEFAULT TRUE,
    metadata    JSONB,
    UNIQUE (symbol, category)
);

CREATE TABLE IF NOT EXISTS account
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

CREATE TABLE IF NOT EXISTS source_system
(
    id                    BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(50) NOT NULL,
    code                  VARCHAR(20) NOT NULL UNIQUE,
    anchored_account_id   BIGINT NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_source_system_account FOREIGN KEY (anchored_account_id) REFERENCES account (id)
);

CREATE TABLE IF NOT EXISTS transaction
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

CREATE TABLE IF NOT EXISTS ingestion_rule
(
    id                BIGSERIAL PRIMARY KEY,
    match_pattern     VARCHAR(255) NOT NULL,
    target_account_id BIGINT       NOT NULL,
    priority          INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rule_target FOREIGN KEY (target_account_id) REFERENCES account (id)
);

CREATE TABLE IF NOT EXISTS market_quote
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
