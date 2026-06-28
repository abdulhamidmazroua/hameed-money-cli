# HameedMoneyCLI

A personal finance CLI tool for ingesting CSV exports from Egyptian banks and brokers (HSBC, Banque Misr, Thndr), normalising them into a double-entry ledger, and generating net worth reports. Built with Spring Boot 4 + Spring Shell 4 + Java 21.

---

## Philosophy: The Financial Data Lake

The core idea is to treat the system as a **data normaliser**:

- **Input:** Messy CSV exports from different institutions (HSBC Egypt, Thndr, manual logs).
- **Processing:** Source-specific parsers turn every row into a normalised double-entry transaction, with regex-based rules auto-categorising descriptions into target accounts.
- **Storage:** Transactions are stored in a ledger for historical analysis. No balances are stored — everything is derived from the transaction log.
- **Output:** Reports for net worth, cash flow, and portfolio performance, with multi-currency valuation via a graph-based oracle.

---

## Core Concepts

### Account Hierarchy (Root / Parent / Leaf)

Accounts form a pure organisational tree (like folders on a filesystem). The `asset_id` column is **nullable**:

| Level | `asset_id` | Role |
|-------|-----------|------|
| **Root** | `NULL` | Top-level group by master type: `ASSET`, `LIABILITY`, `INCOME`, `EXPENSE`, `SYSTEM` |
| **Parent** | `NULL` | Organisational folder (e.g. "Cash", "Securities", "Thndr Portfolio") — aggregates child balances |
| **Leaf** | set | The actual account where money lives (e.g. "HSBC Current Account" → `EGP`, "CIB Stock" → `COMI.CA`) |

Only leaf accounts hold an asset and can appear in transactions. Parent accounts are unit-less containers — their "balance" is computed on the fly by summing leaf children and converting to the report currency.

```
AccountIs
├── ASSET
│   ├── Cash (Internal)
│   │   ├── HSBC Current Account (EGP)
│   │   ├── Misr Current Account (EGP)
│   │   └── Thndr Investment Account (EGP)
│   ├── Securities (Internal)
│   │   ├── Thndr Portfolio   (folder)
│   │   │   ├── CIB Stock     (COMI.CA)
│   │   │   └── Abu Qir       (ABUK.CA)
│   │   └── Etoro Portfolio   (folder)
│   ├── Fixed (Internal)
│   │   └── Property
│   └── Debt
├── LIABILITY
│   └── Loan Account
├── INCOME
│   ├── Basic Salary
│   └── Bonus
├── EXPENSE
│   ├── Food
│   ├── Groceries
│   ├── Subscriptions
│   ├── PocketMoney
│   ├── MobileRecharge
│   ├── Family
│   ├── Charity
│   └── Lending
└── SYSTEM
    ├── Opening EGP Balance
    ├── EGP Balance Increase Adjustment
    └── EGP Balance Decrease Adjustment
```

Each asset in the system gets its own trio of SYSTEM accounts to provide consistent balancing units for opening balances and reconciliation adjustments. These are created automatically when a leaf account with that asset is first created (see [System Adjustments](#system-adjustments)).

### Internal vs External Accounts

Accounts are divided into two realms:

| Realm | Types | Purpose |
|-------|-------|---------|
| **Internal** (The Vault) | `ASSET`, `LIABILITY` | What you own and owe — components of **net worth** |
| **External** (The World) | `INCOME`, `EXPENSE` | Origins and destinations of money — track **flow**, not balance |
| **System** | `SYSTEM` | Opening balances and reconciliation adjustments — ignored in cash flow reports |

**PocketMoney rule:** Cash withdrawn from an ATM is treated as an **Expense** (External). Once money leaves the tracked banking system, it is considered spent — the physical cash is not tracked digitally.

### The Source-Destination Transaction Model

Every transaction is a movement of value from one account to another:

| Transaction Type | From (Source) | To (Destination) | Effect |
|-----------------|---------------|------------------|--------|
| **Income** | Income (External) | Asset (Internal) | Net worth increases |
| **Spending** | Asset (Internal) | Expense (External) | Net worth decreases |
| **Internal Transfer** | Asset A (Internal) | Asset B (Internal) | Net worth unchanged |
| **Cash Out** | Asset (Internal) | PocketMoney (External) | Net worth decreases (treated as spending) |

### Derived Balance Strategy

Balances are **never stored**. They are computed from the transaction ledger on demand. This makes the system audit-proof — deleting or editing a transaction automatically updates all reports without risk of desync.

The formula depends on the account's `master_type`:

| Master Type | Balance Formula | Logic |
|------------|----------------|-------|
| `ASSET` | `∑to_amount - ∑from_amount` | Money in increases wealth |
| `EXPENSE` | `∑to_amount - ∑from_amount` | Money in increases total spending |
| `LIABILITY` | `∑from_amount - ∑to_amount` | Borrowing (money out) increases debt |
| `INCOME` | `∑from_amount - ∑to_amount` | Earning (money out) increases income |
| `SYSTEM` | `∑from_amount - ∑to_amount` | Same direction as Liability/Income |

### System Adjustments

Opening balances and reconciliation corrections use the `is_system_adjustment` flag on transactions. This keeps cash flow reports clean:

- **Net worth report:** includes system adjustments (the money is in your account).
- **Income/Expense report:** filters system adjustments out (your opening balance won't appear as "income").

Each asset used in the ledger gets a dedicated trio of SYSTEM accounts. They are auto-created when a leaf account referencing that asset is first created (via `account create`). The naming convention is:

| Symbol | Opening Balance | Increase Adjustment | Decrease Adjustment |
|--------|----------------|---------------------|---------------------|
| `EGP` | `Opening EGP Balance` | `EGP Balance Increase Adjustment` | `EGP Balance Decrease Adjustment` |
| `COMI` | `Opening COMI Balance` | `COMI Balance Increase Adjustment` | `COMI Balance Decrease Adjustment` |

This ensures every transaction leg uses the same unit (e.g. opening EGP cash uses `Opening EGP Balance`, opening COMI shares uses `Opening COMI Balance`), so account balances are always mathematically meaningful.

**Workflow:**

1. **Initialise an account:**
   `hmc init --account "HSBC" --balance 50000`
    → Creates a transaction: `Opening EGP Balance (SYSTEM)` → `HSBC Current Account (ASSET)` with `is_system_adjustment = TRUE`

2. **Reconcile a difference:**
   `hmc reconcile --account "HSBC" --actual 49990`
    → Creates a transaction: `HSBC Current Account (ASSET)` → `EGP Balance Decrease Adjustment (SYSTEM)` with `is_system_adjustment = TRUE`

### Universal Graph Oracle (Multi-Currency Valuation)

The system treats asset valuation as a **graph traversal** problem:

- **Nodes:** Every `Asset` (EGP, USD, AAPL, Gold) is a node.
- **Edges:** `market_quote` records are directed edges with a price weight (e.g. USD → EGP = 48.5).
- **Triangulation:** To value AAPL in EGP, the engine finds a path: AAPL → USD → EGP, multiplying edge weights along the way.
- **Identity edges:** An asset always converts to itself at 1:1.
- **Inverse edges:** If USD→EGP = 48.5, the reverse EGP→USD = 1/48.5 is derived automatically.

The FinancialOracle uses BFS to find the shortest conversion path between any two assets, enabling fully multi-currency net worth reports.

### Idempotency

Each transaction is assigned an `external_ref_id` — a SHA-256 hash of `sourceSystemCode|date|description|amount`. Running `ingest` multiple times on the same file produces **zero duplicates**.

---

## Getting Started

### Prerequisites

- Java 21+
- PostgreSQL (create a database matching `application.properties`)
- Python 3 + [`yfinance`](https://pypi.org/project/yfinance/) (`pip install yfinance`) for market quote fetching via `quote fetch`
- (Optional) [TwelveData](https://twelvedata.com) API key — only needed if you set `hmc.market.data.provider.default=twelvedata` (see [Market Data Provider](#market-data-provider))
- (Optional) [EODHD](https://eodhd.com) API key — set `EODHD_API_KEY` in `.env` (pre-configured, only needed if the default key is exhausted)

### Setup

```bash
# 1. Start the application (schema and seed data load automatically)
./mvnw spring-boot:run
```

On first launch, the schema (`app_data` schema, 6 tables) and seed data (assets, accounts, source systems, sample rules) are loaded from `src/main/resources/db/init.sql`.

### Typical Workflow

```
1. Sync tradable instruments   ──►  asset fetch
2. Register manual assets      ──►  asset register
3. Create accounts             ──►  account create
4. Set up opening balances     ──►  transaction add (SYSTEM_ADJUSTMENT)
5. Set market quotes for FX    ──►  quote fetch / quote set
6. Import CSV exports          ──►  ingest
7. Create rules for uncategorised  ──►  rule add
8. Run reports                 ──►  report nw
```

---

## General Onboarding Walkthrough

Setting up a new position in the ledger follows this general flow:

```
asset fetch / asset register  →  account create  →  hmc init (opening balance)  →  quote fetch / quote set  →  report nw
```

### Step 1: Sync tradable instruments from the provider

```bash
asset fetch --category stock --exchange NASDAQ
asset fetch --category etf --exchange LSE
```

This pulls listed stocks/ETFs/funds from the configured market data provider (EODHD or TwelveData) and stores them in the `asset` table with their ISIN, currency, and exchange metadata.

Supported exchanges: EGX, NASDAQ, NYSE, LSE, TSE, HKEX, ASX, TSX, BSE, NSE, TADAWUL, ADX, DFM, QE, EURONEXT, SSE, FWB, SIX, KRX, JPX.

### Step 2: Manually register assets not found by the provider

Some instruments — particularly mutual funds, local closed-end funds, or OTC securities — may not appear in the provider's exchange-symbol-list. Register them manually:

```bash
asset register --name "Some Mutual Fund" --symbol XYZ
```

Prompts for category — choose from `stock`, `etf`, `fund`, `cash`, `crypto`, `commodity`.

### Step 3: Create a leaf account

```bash
account create --name "XYZ Fund Account" --parent-account-id <parent-id>
```

Prompts for:
- **Account Type** — typically `ASSET` for investment positions
- **Asset** — the registered asset to denominate this account

This automatically creates a SYSTEM account trio for the asset (if one doesn't already exist):
- `Opening <symbol> Balance`
- `<symbol> Balance Increase Adjustment`
- `<symbol> Balance Decrease Adjustment`

### Step 4: Record the opening balance

```bash
hmc init --account "XYZ Fund Account" --balance 1000
```

This creates a `SYSTEM_ADJUSTMENT` transaction from the asset's `Opening <symbol> Balance` account to the leaf account with `is_system_adjustment = TRUE`, keeping it out of income/expense reports. The SYSTEM account is automatically resolved from the leaf account's asset.

### Step 5: Set a market quote

For securities, fetch from Yahoo Finance automatically:

```bash
quote fetch --base XYZ --quote USD
```

For assets without a Yahoo Finance listing (or for manual valuation), set the price directly:

```bash
quote set --base XYZ --quote USD --price 12.50 --date 2025-01-15
```

### Step 6: Generate the net worth report

```bash
report nw -c USD
```

The report converts every leaf balance to the target currency via the FinancialOracle graph and prints `totalAssets`, `totalLiabilities`, and `netWorth`.

---

## Making the Flow Seamless (Proposed Plan)

The current flow works but requires too many manual lookups and command invocations. Here is a plan to reduce friction:

### Short-term wins (minimal code, high impact)

| Pain point | Solution |
|------------|----------|
| Need to look up account IDs before every command | Add `--name` / `--symbol` flags alongside `--account-id` / `--asset-id` for all commands, with interactive fuzzy-picking fallback. E.g. `transaction add` accepts `--from-account "Opening XYZ Balance"` instead of `--from-account-id 42` |
| Opening balance requires 3 commands (register, create, add tx) | Add `account init --name "XYZ Account" --parent "Portfolio" --asset "XYZ" --balance 1000` — a single command that registers the asset if missing, creates the leaf account (and SYSTEM trio), and posts the opening balance transaction |
| `report nw` needs `-c` flag | Accept positional arg: `report nw USD` works as a shorthand |
| Asset registration is interactive-only for category | Add `--category` flag to `asset register` so it can be done non-interactively |

### Medium-term (new capabilities)

| Capability | Description |
|------------|-------------|
| **CSV-based onboarding** | `account import --file positions.csv` — batch-import a list of symbols, names, categories, and balances from a spreadsheet. Creates assets, accounts, and opening balance transactions in one shot |
| **Interactive TUI dashboard** | A `dashboard` command that shows the account tree, latest quotes, and a "Quick Actions" menu (add balance → pick account → enter amount → done) |
| **Balance assertion / reconciliation** | `account reconcile --id 5 --actual 50000` — compares the computed balance with the actual figure, prompts to create an adjustment transaction if they differ |
| **Quote auto-warming** | On `asset fetch`, automatically fetch and store market quotes for newly synced instruments so they are ready for reporting immediately |

### Long-term (architectural)

| Improvement | Rationale |
|-------------|-----------|
| **Rename system accounts on asset rename** | If an asset symbol changes, the SYSTEM account names become stale. An `@EventListener` on asset update would rename them automatically |
| **System account lifecycle management** | When the last leaf account for an asset is deleted, offer to clean up orphaned SYSTEM accounts |
| **Undo / rollback support** | Wrapping each write operation in a named transaction that can be rolled back would let users recover from mistakes without manual compensating entries |

### Quick wins I can implement now

If you want, I can start with these:

1. `asset register --category stock` — non-interactive mode
2. `account init` — single-shot account + opening balance setup
3. Positional arg for `report nw EGP`
4. `--name` / `--symbol` option aliases for ID-based commands

Which of these would you like me to tackle?

---

## Commands

### Asset Management

| Command | Description |
|---------|-------------|
| `cat-list` | List all available asset categories |
| `asset list` | List all registered assets |
| `asset register --name <name> --symbol <symbol>` | Register a new asset (prompts for category) |
| `asset fetch --category <category> --exchange <exchange>` | Sync instrument listings from the configured data provider. Categories: `stock`, `etf`, `fund`. Supported exchanges: EGX, NASDAQ, NYSE, LSE, TSE, HKEX, ASX, TSX, BSE, NSE, TADAWUL, ADX, DFM, QE, EURONEXT, SSE, FWB, SIX, KRX, JPX |

### Account Management

| Command | Description |
|---------|-------------|
| `account list` | List all accounts in a colour-coded hierarchical tree, grouped by master type |
| `account create --name <name> --parent-account-id <id>` | Create an account (prompts for account type and asset; leave asset blank for folder accounts) |

### Transaction Management

| Command | Description |
|---------|-------------|
| `transaction add -F <from-id> -T <to-id> -a <amount> -d <date>` | Add a transaction (prompts for type). Use `-a` for equal amounts, or `-f`/`-t` for different amounts |
| `transaction list [-T <type>] [-f <from>] [-t <to>] [-s <start>] [-e <end>]` | List/filter transactions in a formatted table |
| `transaction report [-T <type>] [-f <from>] [-t <to>] [-s <start>] [-e <end>]` | Export filtered transactions to CSV at `~/hmc/reports/` |

**`transaction add` options:**

| Flag | Long Name | Required | Description |
|------|-----------|----------|-------------|
| `-a` | `--amount` | No | Same amount for both sides (alternative to -f/-t) |
| `-f` | `--from-amount` | No | Amount leaving the source |
| `-t` | `--to-amount` | No | Amount entering the destination |
| `-d` | `--date` | Yes | Date (dd-MM-yyyy) |
| `-D` | `--description` | No | Description |
| `-F` | `--from-account-id` | Yes | Source account ID |
| `-T` | `--to-account-id` | Yes | Destination account ID |
| `-e` | `--fee-amount` | No | Fee (default: 0) |

### Ingestion

| Command | Description |
|---------|-------------|
| `ingest -s <source> -f <file-path>` | Parse and import a CSV file from a supported source |

**Supported sources:** `HSBC_APP`, `BANQUE_MISR_APP`, `THNDR_APP`

CSV rows are parsed by source-specific strategies, matched against regex ingestion rules for auto-categorisation, and deduplicated via SHA-256 hash. Unmatched descriptions trigger an interactive prompt to create new rules.

### Ingestion Rules

| Command | Description |
|---------|-------------|
| `rule add --pattern <regex> --target <account-id>` | Add a regex rule for auto-categorising transaction descriptions |

Rules are evaluated in priority order. When a description matches, the transaction is routed to the target account.

### Market Quotes

| Command | Description |
|---------|-------------|
| `quote list` | List the latest stored quote for each asset pair |
| `quote fetch --base <symbol> --quote <symbol>` | Auto-fetch latest price via Python [`yfinance`](https://pypi.org/project/yfinance/) (free, no API key). Handles stocks, forex, crypto, and commodities |
| `quote set --base <symbol> --quote <symbol> --price <value> [--date <date>]` | Manually store a market quote (e.g. USD→EGP = 48.5) |
| `quote get --base <symbol> --quote <symbol>` | Retrieve stored quotes for an asset pair |

**Valid `quote fetch` directions:**

| Base → Quote | Yahoo Symbol | Example |
|---|---|---|
| STOCK / ETF / FUND → CASH | `{exchange symbol}` + exchange suffix | `COMI.CA` (EGX), `AAPL` (NASDAQ) |
| CASH → CASH | `{base}{quote}=X` | `USDEGP=X` |
| CRYPTO → CASH | `{symbol}-{currency}` | `BTC-USD` |
| COMMODITY → CASH | `{symbol}=F` | `GC=F` (gold) |

For stocks, ETFs, and mutual funds, the Yahoo symbol is constructed from the asset symbol + the exchange's Yahoo suffix.
The suffix is determined by looking up `asset.metadata.exchange` (from `asset fetch`) in the `StockExchange` enum:

| Exchange | Yahoo Suffix | Example Symbols |
|----------|-------------|----------------|
| EGX | `.CA` | COMI.**CA**, ABUK.**CA** |
| NASDAQ / NYSE | *(none)* | AAPL, MSFT |
| LSE (London) | `.L` | HSBA.**L** |
| TSE (Tokyo) | `.T` | 7203.**T** |
| HKEX (Hong Kong) | `.HK` | 0700.**HK** |
| ASX (Australia) | `.AX` | BHP.**AX** |
| TSX (Toronto) | `.TO` | SHOP.**TO** |
| BSE (India) | `.BO` | RELIANCE.**BO** |
| NSE (India) | `.NS` | RELIANCE.**NS** |
| TADAWUL (Saudi) | `.SR` | 2222.**SR** |
| ADX / DFM (UAE) | `.AE` | EMAAR.**AE** |
| QE (Qatar) | `.QA` | QNBK.**QA** |
| EURONEXT | `.PA` | MC.**PA** |
| SSE (Shanghai) | `.SS` | 600519.**SS** |
| FWB (Frankfurt) | `.DE` | SAP.**DE** |
| SIX (Swiss) | `.SW` | NESN.**SW** |
| KRX (Korea) | `.KS` | 005930.**KS** |
| JPX (Japan) | `.T` | 9984.**T** |

If the symbol already ends with the correct suffix (as TwelveData returns them), no duplication occurs.
Manually registered stocks without exchange metadata fall back to the bare symbol. Invalid directions (e.g. CASH → STOCK, STOCK → STOCK, PROPERTY → anything) are rejected with a suggestion.

### Reporting

| Command | Description |
|---------|-------------|
| `report nw --currency <currency>` | Generate a net worth / balance sheet report valued in the given currency |
| `report data-integrity` | Generate a data integrity audit report showing opening balance totals, manual adjustments, and system health % |

The net worth report:
1. Finds all leaf accounts (ASSET and LIABILITY types).
2. Computes balances from the transaction ledger.
3. Converts all balances to the target currency via the FinancialOracle graph.
4. Prints `totalAssets`, `totalLiabilities`, and `netWorth`.

### Audit

| Command | Description |
|---------|-------------|
| `audit account --id <id>` or `--name <name>` | Audit a specific account — displays computed balance, transaction count, and date range. Omitting both flags opens an interactive picker |
| `audit trail` | Full ledger audit — checks all leaf accounts for anomalies (negative ASSET balances, orphaned transactions) |

### System Adjustments

| Command | Description |
|---------|-------------|
| `hmc init --account <name> --balance <amount>` | Initialize a leaf account with an opening balance. Creates a SYSTEM_ADJUSTMENT transaction from the asset's `Opening <symbol> Balance` account |
| `hmc reconcile --account <name> --actual <amount>` | Reconcile a leaf account's computed balance to the given actual. Creates an increase or decrease SYSTEM_ADJUSTMENT transaction as needed |

---

## Reporting Model

| Report | What it computes | Formula |
|--------|-----------------|---------|
| **Net Worth** | Wealth snapshot | `∑(Internal Asset balances) - ∑(Internal Liability balances)`, all converted to target currency via the Oracle graph |
| **Cash Flow** | Income vs Expenses | `∑(Income outflows) - ∑(Expense inflows)` for a period. System adjustments are excluded |
| **Portfolio** | Investment growth | Buy price vs market price per stock, with currency devaluation impact separated |
| **Data Integrity** | System health | `1 - (totalAdjustments / totalVolume)` — measures what fraction of the ledger is manual system corrections vs normal activity |

---

## Configuration

Key properties in `src/main/resources/application.properties`:

| Property | Description |
|----------|-------------|
| `spring.datasource.url` | PostgreSQL JDBC URL (`jdbc:postgresql://localhost:5432/hmc-db`) |
| `spring.datasource.username` | DB username (`hmc-user`) |
| `spring.datasource.password` | DB password (`hmc-password`) |
| `spring.datasource.hikari.schema` | DB schema (`app_data`) |
| `hmc.report.output-dir` | CSV report output directory (default: `~/hmc/reports`) |
| `hmc.market.data.provider.default` | Market data provider: `eodhd` (default) or `twelvedata` |
| `hmc.market.data.provider.eodhd.api-key` | EODHD API key — set via `EODHD_API_KEY` env var or `.env` |
| `hmc.market.data.provider.twelve-data.api-key` | TwelveData API key for stock listings — set via `TWELVE_DATA_API_KEY` env var |

### Market Data Provider

`asset fetch` uses a pluggable `MarketDataProvider` interface. The active provider is selected via `hmc.market.data.provider.default` in `application.properties`:

- **`eodhd`** (default) — fetches stocks, ETFs, and mutual funds from EODHD. Set `EODHD_API_KEY` in `.env`.
- **`twelvedata`** — fetches stocks only from TwelveData. Requires `TWELVE_DATA_API_KEY` env var.

To switch providers:
```properties
hmc.market.data.provider.default=twelvedata
```

---

## Project Structure

```
src/main/java/org/hameed/hameedmoneycli/
├── commands/        # Spring Shell command definitions
├── config/          # Spring beans, RestClient, strategy wiring
├── enums/           # AccountType, AssetCategory, SourceSystemCode, StockExchange, TransactionType
├── ingestion/       # CSV parsing helpers and regex rule pattern factory
├── model/
│   ├── dto/         # Request/response DTOs
│   └── entity/      # JPA entities (Account, Asset, Transaction, MarketQuote, etc.)
├── proxy/           # Market data providers (EODHD, TwelveData) + Yahoo Finance
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic layer
└── util/            # Ingestion strategy interface + implementations
```

### Database Tables

| Table | Purpose |
|-------|---------|
| `asset` | Master list of financial instruments and currencies |
| `account` | Hierarchical accounts (self-referencing parent_id, nullable asset_id) |
| `source_system` | Data import sources (HSBC, Banque Misr, Thndr, Manual) |
| `transaction` | Double-entry ledger (source-destination with separate amounts and fees) |
| `ingestion_rule` | Regex patterns mapping descriptions to target accounts |
| `market_quote` | FX rates and stock prices for the FinancialOracle graph |

---

## Building

```bash
./mvnw clean package              # Build executable JAR
./mvnw -Pnative native:compile    # Build native image (requires GraalVM)
```

---

*Design documentation is available in `hmc-docs/` for deeper dives into specific topics.*
