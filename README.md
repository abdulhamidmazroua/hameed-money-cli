# HameedMoneyCLI

> A double-entry personal finance ledger for the command line. Ingest CSV exports from any bank or investment platform, normalise into a transaction log, and generate multi-currency reports.

Built with **Spring Boot 4 + Spring Shell 4 + GraalVM 25**, powered by **PostgreSQL**, and **yfinance** for free stock/forex quotes.

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

## Setup

### Prerequisites

- **Docker** — for running PostgreSQL
- **GraalVM 25+** with `native-image` — install via SDKMAN: `sdk install java 25.0.2-graalce`
- **Python 3 + yfinance** — `pip install yfinance` (for `quote fetch`)

### One-command install

```bash
./scripts/install.sh
```

This will:
1. Start PostgreSQL via Docker Compose
2. Build the native binary
3. Install `hmc` to `~/.local/bin`
4. Run database migrations

After install, use `hmc` from anywhere.

### Manual steps (if you prefer)

```bash
./mvnw -Pnative native:compile -DskipTests       # Build native binary
cp target/hameed-money-cli ~/.local/bin/hmc      # Install to PATH
hmc help                                          # Creates ~/.hmc/hmc.db + runs migrations on first startup
```

### Configuration via environment variables

Copy `.env.example` to `.env` and fill in your API keys:

| Variable | Default | Description |
|----------|---------|-------------|
| `HMC_DB_URL` | `jdbc:postgresql://localhost:5432/hmc-db` | Database connection |
| `HMC_DB_USER` | `hmc-user` | Database user |
| `HMC_DB_PASSWORD` | `hmc-password` | Database password |
| `HMC_DB_SCHEMA` | `app_data` | Database schema |
| `HMC_REPORT_OUTPUT_DIR` | `~/hmc/reports` | CSV export directory |
| `HMC_MARKET_DATA_PROVIDER` | `eodhd` | Provider for `asset fetch` (`eodhd` or `twelvedata`) |
| `EODHD_API_KEY` | *(none)* | EODHD API key |
| `TWELVE_DATA_API_KEY` | *(none)* | Twelve Data API key |

### Database backup & restore

```bash
hmc db backup --output ~/hmc/backups     # From inside the CLI
./scripts/backup.sh                       # Or from the terminal
./scripts/restore.sh ~/hmc/backups/hmc-20260101_120000.sql
```

---

## Quick Start

```bash
# 1. Register a cash asset
hmc asset register "Egyptian Pound" EGP --category cash

# 2. Create your account and post an opening balance (asset must exist first)
hmc account init --name "My Wallet" --asset EGP --balance 5000

# 3. Set a market quote
hmc quote set EGP USD --price 0.020

# 4. See your net worth
hmc report nw EGP
```

---

## Core Concepts

*See sections below for detailed explanations.*

### 1. Account Classification

**Internal Accounts (The "Vault")** — what you own and owe.

| Type | Examples |
|------|----------|
| **ASSET** | Cash, bank accounts, investment portfolios, stocks, an apartment |
| **LIABILITY** | Loans, credit card balances |

**External Accounts (The "World")** — origins and destinations of money.

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

Every entry in the `transaction` table follows: `From Account → To Account`.

| Transaction Type | From (Source) | To (Destination) | Effect |
|-----------------|---------------|------------------|--------|
| **Income** | External (Income) | Internal (Asset) | Both increase |
| **Spending** | Internal (Asset) | External (Expense) | Asset decreases, Expense increases |
| **Internal Transfer** | Internal (Asset A) | Internal (Asset B) | Asset A decreases, Asset B increases |
| **Cash Out** | Internal (Asset) | External (PocketMoney) | Asset decreases, Expense increases |

**System Adjustments:**

| Transaction | From | To | Effect |
|------------|------|----|--------|
| **Opening balance** | SYSTEM | Asset | Asset increases (net worth ↑) |
| **Reconciliation (up)** | SYSTEM | Asset | Asset increases (net worth ↑) |
| **Reconciliation (down)** | Asset | SYSTEM | Asset decreases (net worth ↓) |

### 3. Balance Derivation

Balances are **never stored** — they are computed from the transaction log on demand.

| Master Type | Balance Formula |
|-------------|----------------|
| **ASSET** | `∑to_amount − ∑from_amount` |
| **EXPENSE** | `∑to_amount − ∑from_amount` |
| **LIABILITY** | `∑from_amount − ∑to_amount` |
| **INCOME** | `∑from_amount − ∑to_amount` |
| **SYSTEM** | `∑from_amount − ∑to_amount` |

Maps to the extended accounting equation: `Assets + Expenses = Liabilities + Equity + Income`

### 4. The PocketMoney Rule

Cash withdrawn from an ATM is treated as an **Expense** (External). Once money leaves the tracked banking system, it is considered spent — the physical cash is not tracked digitally.

### 5. Reporting Impact

| Report | What it computes |
|--------|-----------------|
| **Net Worth** | `∑(ASSET balances) − ∑(LIABILITY balances)`, all converted to target currency |
| **Cash Flow** | `∑(Income outflows) − ∑(Expense inflows)` for a period |
| **Portfolio** | Market price vs buy price per stock |
| **Data Integrity** | `1 − (manualAdjustments / totalVolume)` |

### Accounts are hierarchical

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

| Level | `asset_id` | Role |
|-------|-----------|------|
| **Root** | `NULL` | Top-level by master type |
| **Parent** | `NULL` | Organisational folder |
| **Leaf** | set | Actual account where money lives |

### System Adjustments

Each asset gets a **dedicated trio of SYSTEM accounts**:

| Asset | Opening Balance | Increase | Decrease |
|-------|----------------|----------|----------|
| `EGP` | `Opening EGP Balance` | `EGP Balance Increase Adjustment` | `EGP Balance Decrease Adjustment` |

Auto-created when a leaf account referencing that asset is first created.

```bash
hmc account init --name "HSBC Current" --asset EGP --balance 50000
  → Opening EGP Balance (SYSTEM) → HSBC Current Account (ASSET)

hmc reconcile "HSBC Current" --actual 49990
  → HSBC Current Account (ASSET) → EGP Balance Decrease Adjustment (SYSTEM)
```

### The Graph Oracle (Multi-Currency Valuation)

Valuation is a **graph traversal** problem:

- **Nodes:** Every `Asset` (EGP, USD, AAPL, Gold) is a node.
- **Edges:** `market_quote` records are directed price edges.
- **Identity:** An asset always converts to itself at 1:1.
- **Inverse:** If USD→EGP = 48.5, then EGP→USD ≈ 1/48.5 is derived automatically.
- **Traversal:** BFS finds the shortest path and multiplies edge weights.

### Idempotency

Every transaction gets an `external_ref_id` — a SHA-256 hash of `sourceSystemCode|date|description|amount`. Running `ingest` multiple times on the same file produces **zero duplicates**.

---

## Walkthrough: Your First Position

### 1. Fetch available instruments

```bash
hmc asset fetch stock EGX
```

Pulls all EGX-listed stocks. Supported categories: `stock`, `etf`, `fund`. Supported exchanges: `EGX`, `NASDAQ`, `NYSE`, `LSE`, `TSE`, `HKEX`, `ASX`, `TSX`, `BSE`, `NSE`, `TADAWUL`, `ADX`, `DFM`, `QE`, `EURONEXT`, `SSE`, `FWB`, `SIX`, `KRX`, `JPX`.

### 2. Create an account

Register the asset first if it doesn't exist:

```bash
hmc asset register "Commercial International Bank" COMI.CA --category stock
```

Then create the account:

```bash
hmc account create --name "CIB Shares" --parent-account-id 5
```

Launches an interactive wizard for account type (choose `ASSET`) and asset (pick `COMI.CA`).

### 3. Set a market quote

```bash
hmc quote fetch COMI.CA EGP
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
hmc quote set MYASSET EGP --price 50000 --date 2025-01-15
```

### 4. Run the net worth report

```bash
hmc report nw EGP
```

### 5. Import CSV exports

```bash
hmc ingest HSBC_APP ~/Downloads/transactions.csv
```

---

## Command Reference

### Asset Management

| Command | What it does |
|---------|--------------|
| `cat-list` | List all asset categories |
| `asset list` | List all registered assets |
| `asset register <name> <symbol> [--category <cat>]` | Register a new asset (`isTradable` is inferred from category) |
| `asset fetch <category> <exchange>` | Sync instruments from the market data provider |

### Account Management

| Command | What it does |
|---------|--------------|
| `account list` | Display the colour-coded account tree |
| `account create --name <n> [--parent-account-id <id>]` | Create an account (interactive pickers) |
| `account init --name <n> --asset <symbol> --balance <amt>` | One-shot: account + SYSTEM trio + opening balance (asset must exist first via `asset register`) |
| `account delete <id>` | Delete an account |

### Transaction Management

| Command | What it does |
|---------|--------------|
| `transaction add -F <from-id> -T <to-id> -a <amount> -d <date>` | Add a transaction |
| `transaction list [-T <type>] [-f <from>] [-t <to>] [-s <start>] [-e <end>]` | List/filter transactions |
| `transaction report [-T <type>] [-f <from>] [-t <to>] [-s <start>] [-e <end>]` | Export to CSV |

### Ingestion & Rules

| Command | What it does |
|---------|--------------|
| `ingest <source> <file-path>` | Parse and import a CSV file |
| `rule add <pattern> <target-id>` | Add a regex rule for auto-categorisation |

### Market Quotes

| Command | What it does |
|---------|--------------|
| `quote fetch <base> <quote>` | Auto-fetch price from Yahoo Finance |
| `quote set <base> <quote> --price <v> [--date <d>]` | Store a manual quote |
| `quote get <base> <quote>` | Retrieve stored quotes |
| `quote list` | List the latest quote for every pair |

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

## Project Structure

```
src/main/java/org/hameed/hameedmoneycli/
├── commands/        # Spring Shell commands
├── config/          # Spring beans, RestClient, strategy wiring
├── enums/           # AccountType, AssetCategory, etc.
├── ingestion/       # CSV parsing helpers + regex rule factory
├── model/
│   ├── dto/         # Request/response DTOs
│   └── entity/      # JPA entities
├── proxy/           # Market data providers + Yahoo Finance
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic layer
└── util/            # Ingestion strategies
```

### Database (6 tables)

| Table | Role |
|-------|------|
| `asset` | Master list of financial instruments and currencies |
| `account` | Hierarchical accounts |
| `source_system` | Data import sources |
| `transaction` | Double-entry ledger |
| `ingestion_rule` | Regex patterns for auto-categorisation |
| `market_quote` | FX rates and security prices |

---

*Design documentation lives in `hmc-docs/` for deeper dives into specific topics.*
