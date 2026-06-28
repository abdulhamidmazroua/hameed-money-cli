# HameedMoneyCLI

> A double-entry personal finance ledger for the command line. Ingest CSV exports from any bank or investment platform, normalise into a transaction log, and generate multi-currency reports.

Built with **Spring Boot 4 + Spring Shell 4 + Java 21**, powered by **PostgreSQL**, and **yfinance** for free stock/forex quotes.

---

## Philosophy: Bring Your Own Bank

HameedMoneyCLI is a **data normalisation framework**, not a bank-specific tool. The system provides the ledger, the double-entry engine, and the reporting — **you** provide the ingestion strategy for your bank, broker, or investment app.

```
[HSBC Statement]  ──►  HSBC Strategy  ──┐
[Thndr Statement] ──►  Thndr Strategy  ──┤
[Your Bank]       ──►  Your Strategy   ──┤
[Mint/Intuit]     ──►  Mint Strategy   ──┘
                                           ▼
                                     Normalised
                                    Double-Entry
                                       Ledger
                                           │
                                           ▼
                                    ┌──────────────┐
                                    │ Net Worth    │
                                    │ Cash Flow    │
                                    │ Portfolio    │
                                    │ Data Health  │
                                    └──────────────┘
```

- **Input:** Messy CSV exports from any source — banks, brokers, crypto exchanges, manual logs.
- **Processing:** You write (or pick from built-in) source-specific parsers. Each row becomes a normalised double-entry transaction. Regex rules auto-categorise descriptions into target accounts.
- **Storage:** Transactions live in a ledger. **No balances are stored** — everything is derived from the transaction log. This makes the system audit-proof by design.
- **Output:** Reports for net worth, cash flow, and portfolio performance. Multi-currency valuation via a graph-based oracle.

Built-in parsers: `HSBC_APP`, `BANQUE_MISR_APP`, `THNDR_APP`. Add your own by implementing the `IngestionStrategy` interface.

---

## Quick Start (5 minutes)

```bash
# 1. Start the app (schema + seed data load automatically)
./mvnw spring-boot:run

# 2. Register a cash asset
asset register "Egyptian Pound" EGP --category cash

# 3. Create your account and post an opening balance in one shot
account init --name "My Wallet" --asset EGP --balance 5000

# 4. Set a market quote
quote set EGP USD --price 0.020

# 5. See your net worth
report nw EGP
```

---

## Core Concepts

### 1. Account Classification

Accounts are divided into two functional realms:

**Internal Accounts (The "Vault")** — what you own and owe. These are the components of your net worth.

| Type | Examples |
|------|----------|
| **ASSET** | Cash, bank accounts, investment portfolios, stocks, an apartment |
| **LIABILITY** | Loans, credit card balances |

**External Accounts (The "World")** — the origins and destinations of your money. These track **flow**, not balance.

| Type | Examples |
|------|----------|
| **INCOME** | Salary, bonuses, dividends |
| **EXPENSE** | Groceries, subscriptions, food, pocket money |

**External accounts cannot receive a transfer from another External account** — money always flows between Internal and External (or between two Internal accounts). This ensures the ledger cleanly separates "what you have" from "where it comes from and goes to."

A third technical type exists for internal plumbing:

| Type | Purpose |
|------|---------|
| **SYSTEM** | Opening balances and reconciliation adjustments — ignored in cash flow reports |

### 2. The Transaction Framework (Source → Destination)

Every entry in the `transaction` table follows the rule:

```
From Account  →  To Account
```

The effect on each account's balance depends on its master type:

| Transaction Type | From (Source) | To (Destination) | Effect |
|-----------------|---------------|------------------|--------|
| **Income** | External (Income) | Internal (Asset) | Both increase. You earned more income, and your bank balance grew. |
| **Spending** | Internal (Asset) | External (Expense) | Asset decreases, Expense increases. Your bank balance dropped, and your total spending grew. |
| **Internal Transfer** | Internal (Asset A) | Internal (Asset B) | Asset A decreases, Asset B increases. Net worth stays the same (e.g. moving money between accounts). |
| **Cash Out** | Internal (Asset) | External (PocketMoney) | Asset decreases, Expense increases. Money left the tracked system. |

**System Adjustments** follow the same Source → Destination pattern:

| Transaction | From | To | Effect |
|------------|------|----|--------|
| **Opening balance** | SYSTEM | Asset | Asset increases (net worth ↑) |
| **Reconciliation (up)** | SYSTEM | Asset | Asset increases (net worth ↑) |
| **Reconciliation (down)** | Asset | SYSTEM | Asset decreases (net worth ↓) |

### 3. Balance Derivation

Balances are **never stored** — they are computed from the transaction log on demand. This makes the system audit-proof: deleting or editing a transaction automatically updates all reports.

The formula depends on the master type:

| Master Type | Balance Formula | Logic |
|-------------|----------------|-------|
| **ASSET** | `∑to_amount − ∑from_amount` | Money coming in (debit) increases your wealth |
| **EXPENSE** | `∑to_amount − ∑from_amount` | Money coming in increases your total spending |
| **LIABILITY** | `∑from_amount − ∑to_amount` | Money leaving (credit) increases your debt |
| **INCOME** | `∑from_amount − ∑to_amount` | Money leaving increases your total income |
| **SYSTEM** | `∑from_amount − ∑to_amount` | Same logic as Liability/Income |

This maps directly to the extended accounting equation:

```
Assets + Expenses = Liabilities + Equity + Income
```

Accounts on the **left side** (ASSET, EXPENSE) increase with debits (money flowing **to** them). Accounts on the **right side** (LIABILITY, INCOME) increase with credits (money flowing **from** them). Every transaction preserves this balance — the sum of all debits always equals the sum of all credits.

### 4. The PocketMoney Rule

Cash withdrawn from an ATM is treated as an **Expense** (External). Once money leaves the tracked banking system, it is considered spent — the physical cash is not tracked digitally. In reports, you won't look at the "Balance" of PocketMoney to see how much cash you have; you will look at the **total inflow** to PocketMoney to see how much untracked cash you've used this month.

### 5. Reporting Impact

These classifications directly feed every report:

| Report | What it computes | Formula |
|--------|-----------------|---------|
| **Net Worth** | Wealth snapshot | `∑(ASSET balances) − ∑(LIABILITY balances)`, all converted to target currency |
| **Cash Flow** | Income vs Expenses | `∑(Income outflows) − ∑(Expense inflows)` for a period. System adjustments excluded |
| **Portfolio** | Investment growth | Market price vs buy price per stock |
| **Data Integrity** | Ledger health | `1 − (manualAdjustments / totalVolume)` — higher is better |

### Accounts are hierarchical

| Level | `asset_id` | Role |
|-------|-----------|------|
| **Root** | `NULL` | Top-level by master type (`ASSET`, `LIABILITY`, `INCOME`, `EXPENSE`, `SYSTEM`) |
| **Parent** | `NULL` | Organisational folder — aggregates child balances |
| **Leaf** | set | The actual account where money lives. Only leaf accounts appear in transactions |

Example tree:

```
ASSET
├── Cash
│   ├── HSBC Current Account (EGP)
│   ├── Checking Account (USD)
│   └── Interactive Brokers (USD)
├── Securities
│   ├── Thndr Portfolio
│   │   ├── CIB Stock (COMI.CA)
│   │   └── EFG Hermes (EFGH.CA)
│   └── Robinhood
│       ├── Apple (AAPL)
│       └── VOO (VOO)
└── Real Estate
    └── My Apartment (APT)
```

### System Adjustments

Opening balances and reconciliation corrections use the `is_system_adjustment` flag. Each asset gets a **dedicated trio of SYSTEM accounts**, ensuring every leg uses the same denomination:

| Asset | Opening Balance | Increase | Decrease |
|-------|----------------|----------|----------|
| `EGP` | `Opening EGP Balance` | `EGP Balance Increase Adjustment` | `EGP Balance Decrease Adjustment` |
| `COMI.CA` | `Opening COMI.CA Balance` | `COMI.CA Balance Increase Adjustment` | `COMI.CA Balance Decrease Adjustment` |

These are auto-created when a leaf account referencing that asset is first created.

**Workflow:**
```bash
# Initialize an account
hmc init "HSBC Current" --balance 50000
  → Opening EGP Balance (SYSTEM) → HSBC Current Account (ASSET)

# Reconcile a discrepancy
hmc reconcile "HSBC Current" --actual 49990
  → HSBC Current Account (ASSET) → EGP Balance Decrease Adjustment (SYSTEM)
```

System adjustments are the **only** way to post opening balances and corrections. They are filtered out of income/expense reports automatically.

### The Graph Oracle (Multi-Currency Valuation)

Valuation is a **graph traversal** problem:

- **Nodes:** Every `Asset` (EGP, USD, AAPL, Gold) is a node.
- **Edges:** `market_quote` records are directed price edges (USD → EGP = 48.5).
- **Identity:** An asset always converts to itself at 1:1.
- **Inverse:** If USD→EGP = 48.5, then EGP→USD ≈ 1/48.5 is derived automatically.
- **Traversal:** BFS finds the shortest path between any two assets (e.g. AAPL → USD → EGP) and multiplies edge weights.

This powers fully multi-currency net worth reports: you can hold USD stocks, EGP cash, and a EUR-denominated property and see everything converted to a single currency.

### Idempotency

Every transaction gets an `external_ref_id` — a SHA-256 hash of `sourceSystemCode|date|description|amount`. Running `ingest` multiple times on the same file produces **zero duplicates**.

---

## Setup

### Prerequisites

- **Java 21+**
- **PostgreSQL** — create a database (default: `jdbc:postgresql://localhost:5432/hmc-db`)
- **Python 3 + yfinance** — `pip install yfinance` (for `quote fetch`)
- **(Optional) EODHD API key** — set `EODHD_API_KEY` in `.env` if the default key is exhausted
- **(Optional) TwelveData API key** — set `TWELVE_DATA_API_KEY` in `.env` if you switch providers

### Configuration

Key properties in `src/main/resources/application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/hmc-db` | Database connection |
| `spring.datasource.username` | `hmc-user` | Database user |
| `spring.datasource.password` | `hmc-password` | Database password |
| `spring.datasource.hikari.schema` | `app_data` | Database schema |
| `hmc.report.output-dir` | `~/hmc/reports` | CSV export directory |
| `hmc.market.data.provider.default` | `eodhd` | Provider for `asset fetch` (`eodhd` or `twelvedata`) |

Schema and seed data load automatically on first launch from `src/main/resources/db/init.sql`.

---

## Walkthrough: Your First Position

Let's add a real stock position and see your net worth.

### 1. Fetch available instruments

```bash
asset fetch stock EGX
```

Pulls all EGX-listed stocks from the market data provider. Supported categories: `stock`, `etf`, `fund`. Supported exchanges: `EGX`, `NASDAQ`, `NYSE`, `LSE`, `TSE`, `HKEX`, `ASX`, `TSX`, `BSE`, `NSE`, `TADAWUL`, `ADX`, `DFM`, `QE`, `EURONEXT`, `SSE`, `FWB`, `SIX`, `KRX`, `JPX`.

> **Manual registration:** For instruments not on the exchange list:
> ```bash
> asset register "My Fund" MYFUND --category fund
> ```

### 2. Create an account

```bash
account create --name "CIB Shares" --parent-account-id 5
```

Launches an interactive wizard for account type (choose `ASSET`) and asset (pick `COMI.CA`).

> **One-shot shortcut:**
> ```bash
> account init --name "CIB Shares" --asset COMI.CA --balance 100
> ```
> Registers the asset if missing, creates the account + SYSTEM trio, and posts the opening balance in a single command.

### 3. Set a market quote

```bash
quote fetch COMI.CA EGP
```

Yahoo symbol construction:

| Direction | Pattern | Example |
|-----------|---------|---------|
| Stock → Cash | `{symbol}` + exchange suffix | `COMI.CA` |
| Cash → Cash | `{base}{quote}=X` | `USDEGP=X` |
| Crypto → Cash | `{symbol}-{currency}` | `BTC-USD` |
| Commodity → Cash | `{symbol}=F` | `GC=F` |

For manual assets:
```bash
quote set MYASSET EGP --price 50000 --date 2025-01-15
```

### 4. Run the net worth report

```bash
report nw EGP
```

Converts every leaf balance to the target currency and prints total assets, total liabilities, and net worth.

### 5. Import CSV exports

```bash
ingest HSBC_APP ~/Downloads/transactions.csv
```

The pipeline:
1. Parse rows with a source-specific strategy
2. Match descriptions against regex rules for auto-categorisation
3. Deduplicate via SHA-256 hash
4. Prompt interactively for unmatched descriptions to create new rules

---

## Command Reference

### Asset Management

| Command | What it does |
|---------|--------------|
| `cat-list` | List all asset categories |
| `asset list` | List all registered assets |
| `asset register <name> <symbol> [--category <cat>]` | Register a new asset (prompts for category if omitted) |
| `asset fetch <category> <exchange>` | Sync instruments from the market data provider |

### Account Management

| Command | What it does |
|---------|--------------|
| `account list` | Display the colour-coded account tree |
| `account create --name <n> [--parent-account-id <id>] [--parent-account-name <n>]` | Create an account (interactive type/asset pickers) |
| `account init --name <n> --asset <symbol> --balance <amt> [--parent-account-id] [--category]` | One-shot: create account + SYSTEM trio + post opening balance |
| `account delete <id>` | Delete an account (checks FK dependencies) |

### Transaction Management

| Command | What it does |
|---------|--------------|
| `transaction add -F <from-id> -T <to-id> -a <amount> -d <date>` | Add a transaction (use `-N`/`-M` for names instead of IDs) |
| `transaction list [-T <type>] [-f <from>] [-t <to>] [-s <start>] [-e <end>]` | List/filter transactions |
| `transaction report [-T <type>] [-f <from>] [-t <to>] [-s <start>] [-e <end>]` | Export to CSV (`~/hmc/reports/`) |

**`transaction add` options:**

| Flag | Long | Required | Description |
|------|------|----------|-------------|
| `-a` | `--amount` | No | Same amount both sides |
| `-f` / `-t` | `--from-amount` / `--to-amount` | No* | Amount leaving / entering |
| `-d` | `--date` | No | Defaults to today |
| `-D` | `--description` | No | Free text |
| `-F` / `-T` | `--from-account-id` / `--to-account-id` | No* | By ID |
| `-N` / `-M` | `--from-account-name` / `--to-account-name` | No* | By name |
| `-e` | `--fee-amount` | No | Default 0 |

\* Either the ID or name flag must be provided for each side.

### Ingestion & Rules

| Command | What it does |
|---------|--------------|
| `ingest <source> <file-path>` | Parse and import a CSV file |
| `rule add <pattern> <target-id>` | Add a regex rule for auto-categorisation |

Built-in sources: `HSBC_APP`, `BANQUE_MISR_APP`, `THNDR_APP`. Add your own by implementing `IngestionStrategy`.

### Market Quotes

| Command | What it does |
|---------|--------------|
| `quote fetch <base> <quote>` | Auto-fetch price from Yahoo Finance |
| `quote set <base> <quote> --price <v> [--date <d>]` | Store a manual quote |
| `quote get <base> <quote>` | Retrieve stored quotes |
| `quote list` | List the latest quote for every pair |

**Exchange suffixes** (used by `quote fetch` for securities):

| Exchange | Suffix | Exchange | Suffix |
|----------|--------|----------|--------|
| EGX | `.CA` | BSE / NSE | `.BO` / `.NS` |
| NASDAQ / NYSE | *(none)* | TADAWUL | `.SR` |
| LSE | `.L` | ADX / DFM | `.AE` |
| TSE | `.T` | QE (Qatar) | `.QA` |
| HKEX | `.HK` | EURONEXT | `.PA` |
| ASX | `.AX` | SSE | `.SS` |
| TSX | `.TO` | FWB / SIX | `.DE` / `.SW` |
| | | KRX / JPX | `.KS` / `.T` |

### System Adjustments

| Command | What it does |
|---------|--------------|
| `hmc init <account-name> --balance <amount>` | Post an opening balance |
| `hmc reconcile <account-name> --actual <amount>` | Fix a balance discrepancy |

### Reporting & Audit

| Command | What it does |
|---------|--------------|
| `report nw [<currency>]` | Net worth report (defaults to EGP) |
| `report data-integrity` | Ledger health check |
| `audit account [<id-or-name>]` | Audit a single account |
| `audit trail` | Full ledger audit |

---

## Reporting Model

| Report | What it computes | Formula |
|--------|-----------------|---------|
| **Net Worth** | Wealth snapshot | `∑(ASSET balances) - ∑(LIABILITY balances)`, all converted to target currency |
| **Cash Flow** | Income vs Expenses | `∑(Income outflows) - ∑(Expense inflows)` for a period |
| **Portfolio** | Investment growth | Market price vs buy price per stock |
| **Data Integrity** | Ledger health | `1 - (manualAdjustments / totalVolume)` |

---

## Configuration

### Profiles

- **default / eodhd** — Fetches stocks, ETFs, and funds from EODHD. Set `EODHD_API_KEY` in `.env`.
- **twelvedata** — Fetches stocks only. Set `TWELVE_DATA_API_KEY` in `.env`.

Switch with:
```properties
hmc.market.data.provider.default=twelvedata
```

---

## Project Structure

```
src/main/java/org/hameed/hameedmoneycli/
├── commands/        # Spring Shell commands
├── config/          # Spring beans, RestClient, strategy wiring
├── enums/           # AccountType, AssetCategory, SourceSystemCode, StockExchange, TransactionType
├── ingestion/       # CSV parsing helpers + regex rule factory
├── model/
│   ├── dto/         # Request/response DTOs and report records
│   └── entity/      # JPA entities (Account, Asset, Transaction, MarketQuote, etc.)
├── proxy/           # Market data providers (EODHD, TwelveData) + Yahoo Finance
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic layer
└── util/            # Ingestion strategy interface + built-in implementations
```

### Database (6 tables)

| Table | Role |
|-------|------|
| `asset` | Master list of financial instruments and currencies |
| `account` | Hierarchical accounts (self-referencing `parent_id`, nullable `asset_id`) |
| `source_system` | Data import sources |
| `transaction` | Double-entry ledger |
| `ingestion_rule` | Regex patterns for auto-categorisation |
| `market_quote` | FX rates and security prices for the Oracle graph |

---

## Building

```bash
./mvnw clean package              # Executable JAR
./mvnw -Pnative native:compile    # Native image (requires GraalVM)
```

---

*Design documentation lives in `hmc-docs/` for deeper dives into specific topics.*
