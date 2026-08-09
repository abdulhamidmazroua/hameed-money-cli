# HameedMoneyCLI

A double-entry personal finance ledger for the command line. Ingest bank statements from any source, auto-classify transactions with rules + LLM, and generate multi-currency reports.

Built with **Spring Boot 4 + Spring Shell 4 + GraalVM 25**, powered by **SQLite**.

---

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Setup](#setup)
- [Core Concepts](#core-concepts)
- [The Ingestion Pipeline](#the-ingestion-pipeline)
- [Ingestion Rules](#ingestion-rules)
- [Walkthrough](#walkthrough-import-a-bank-statement-end-to-end)
- [Command Reference](#command-reference)
- [Project Structure](#project-structure)

---

## Overview

**HameedMoneyCLI** is a personal finance manager that runs entirely in your terminal. It implements **double-entry accounting** — every transaction moves money from one account to another — and computes all balances on demand from the transaction log. This means the ledger is always internally consistent and auditable.

### What it does

| Capability | How it works |
|------------|-------------|
| **Import bank statements** | Convert any format (PDF, image, XLS) to CSV, parse using a configurable layout, then auto-classify every row with regex rules + LLM |
| **Learn from corrections** | Every applied transaction auto-creates a regex rule so future imports from the same source get smarter |
| **Multi-currency** | Assets (EGP, USD, AAPL, Gold) form a graph; BFS finds the shortest conversion path for net worth in any currency |
| **Idempotent imports** | SHA-256 hashing prevents duplicate transactions no matter how many times you run the same file |
| **Hierarchical accounts** | Folders organize your accounts (Checking, Groceries, Salary); only leaf accounts hold assets and appear in transactions |

### Architecture

```
┌─────────────┐     ┌────────────────┐     ┌───────────────┐
│  LLM Proxy  │     │  Rules Engine  │     │ Graph Oracle  │
│  (convert,  │◄───►│  (classify +   │◄───►│  (multi-curr  │
│   classify) │     │   auto-learn)  │     │   valuation)  │
└─────────────┘     └────────────────┘     └───────────────┘
                          │                        │
                     ┌────▼────────────────────────▼─────┐
                     │         SQLite Ledger              │
                     │  (assets, accounts, transactions,  │
                     │   source_systems, market_quotes,   │
                     │   ingestion_rules, staging tables) │
                     └───────────────────────────────────┘
```

The system has three pillars:

1. **Ingestion Pipeline** (`convert` → `source add` → `ingest parse` → `ingest apply`) — takes raw bank data and produces classified ledger entries
2. **Rules Engine** — regex patterns match descriptions to accounts; runs before LLM for speed; auto-creates new rules on every apply
3. **Graph Oracle** — assets are nodes, market quotes are edges; BFS traversal converts any asset to any other for net worth reports

### Account model

Accounts are divided into **internal** (ASSET, LIABILITY — things you own/owe) and **external** (INCOME, EXPENSE, SYSTEM — where money comes from, goes, or plumbing). Transactions flow between internal and external accounts. Balances are derived, never stored.

---

## Quick Start

```bash
# 1. Register a cash asset
asset register "Egyptian Pound" EGP --category cash

# 2. Create an account with an opening balance
account init --name "My Wallet" --asset EGP --balance 5000

# 3. Set a market quote
quote set EGP USD --price 0.020

# 4. See your net worth
report nw EGP

# 5. Import a bank statement
source add --name "My Bank" --code MY_BANK --file statement.csv   # register source
ingest parse MY_BANK statement.csv                                # parse + auto-classify
ingest review --session 1                                         # view results
ingest apply --session 1                                          # commit to ledger
```

---

## Setup

### Prerequisites

- **GraalVM 25+** with `native-image` — `sdk install java 25.0.2-graalce`
- **Python 3** with OCR packages (for PDF/image conversion):

  ```bash
  pip install pypdf pytesseract pdf2image openpyxl Pillow
  brew install tesseract          # macOS only
  ```

  The `convert` command works without these for `.csv` and `.txt` files.

### Installation

```bash
./scripts/install.sh     # builds native binary, installs to ~/.local/bin
```

Or manually:

```bash
cp target/hameed-money-cli ~/.local/bin/hmc
help                  # creates ~/.hmc/ on first launch
```

### Configuration (`~/.hmc/config.json`)

Full example with all options:

```json
{
  "marketDataProvider": "eodhd",
  "eodhd": { "apiKey": "YOUR_EODHD_API_KEY" },
  "twelveData": { "apiKey": "YOUR_TWELVEDATA_API_KEY" },
  "llm": {
    "provider": "ollama",
    "model": "llama3",
    "baseUrl": "http://localhost:11434/api/chat",
    "apiKey": "",
    "classifyPrompt": "Your custom prompt for transaction classification"
  }
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `marketDataProvider` | No | `"eodhd"` or `"twelvedata"` (default: `"eodhd"`) |
| `eodhd.apiKey` | Yes* | EODHD API key. Required when using EODHD |
| `twelveData.apiKey` | Yes* | Twelve Data API key. Required when using Twelve Data |
| `llm` | No | Omit to disable LLM features |
| `llm.provider` | Yes* | `"ollama"`, `"openai"`, `"claude"`, or `"gemini"` |
| `llm.model` | No | Defaults per provider (see table) |
| `llm.baseUrl` | Yes | **Full endpoint URL** for the provider |
| `llm.apiKey` | No | Required for `openai`, `claude`, `gemini` |
| `llm.classifyPrompt` | No | Custom prompt override for classification |

| Provider | Default Model | Example `baseUrl` |
|----------|--------------|-------------------|
| `ollama` | `llama3` | `http://localhost:11434/api/chat` |
| `openai` | `gpt-4o-mini` | `https://api.openai.com/v1/chat/completions` |
| `claude` | `claude-3-haiku-20240307` | `https://api.anthropic.com/v1/messages` |
| `gemini` | `gemini-2.0-flash` | `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent` |

> `baseUrl` is used as-is. You control the model by embedding it in the URL. The `model` field is also sent in the request body for providers that require it.

**Without `llm` section:**
- `ingest parse` uses regex rules only (no LLM classification)
- `convert` will not work
- `source add --file` / `source update-format` cannot auto-detect format

### Database backup

```bash
db backup                           # saves to ~/hmc/backups/
db backup --output /custom/path     # custom output directory
```

---

## Core Concepts

### Account Classification

| Category | Type | Examples |
|----------|------|----------|
| **Internal (what you own/owe)** | ASSET | Cash, bank accounts, stocks, apartment |
| | LIABILITY | Loans, credit card balances |
| **External (where money comes from/goes)** | INCOME | Salary, bonuses, dividends |
| | EXPENSE | Groceries, subscriptions, food |
| **Plumbing** | SYSTEM | Opening balances, reconciliation adjustments |

External accounts cannot receive a transfer from another External account — money always flows between Internal and External (or between two Internal accounts).

### Balance Derivation

Balances are **never stored** — computed from the transaction log on demand.

| Type | Formula |
|------|---------|
| ASSET | `∑to − ∑from` |
| EXPENSE | `∑to − ∑from` |
| LIABILITY | `∑from − ∑to` |
| INCOME | `∑from − ∑to` |
| SYSTEM | `∑from − ∑to` |

### Idempotency

Every transaction gets an `external_ref_id` — a SHA-256 hash of `sourceSystemCode|date|description|amount`. Running `ingest parse` multiple times on the same file produces **zero duplicates**.

### Multi-Currency Valuation

A graph traversal oracle converts between any two assets:

- **Nodes:** Every `Asset` (EGP, USD, AAPL, Gold) is a node
- **Edges:** `market_quote` records are directed price edges
- **Identity:** An asset always converts to itself at 1:1
- **Inverse:** If USD→EGP = 48.5, then EGP→USD ≈ 1/48.5 is derived automatically
- **Traversal:** BFS finds the shortest path and multiplies edge weights

---

## The Ingestion Pipeline

The core workflow is a four-step pipeline:

```
convert ──► source add ──► ingest parse ──► ingest apply
                    ▲                        │
                    │                        ▼
              source update-format    auto-creates rules
```

### 1. `convert` — Normalise any format to CSV

Converts PDFs, images, XLS files, or raw text into clean CSV via LLM. Skips this step if your data is already CSV.

```bash
convert ~/Downloads/statement.pdf
convert ~/Downloads/statement.xls --output cleaned.csv
```

### 2. `source add` / `source update-format` — Register a source system

Each bank or platform is a **source system** with a format config that tells the parser where to find dates, descriptions, and amounts in the CSV.

```bash
# Register with auto-detection (CSV only)
source add --name "My Bank" --code MY_BANK --file statement.csv

# Or create a bare source and detect format later
source add --name "Credit Card" --code CREDIT_CARD
source update-format CREDIT_CARD --file card.csv
```

### 3. `ingest parse` — Parse + auto-classify (one command)

Parses the CSV using the source's format config, then classifies every row:

1. **Regex rules** run first — if a description matches an existing `IngestionRule`, the account is assigned immediately
2. **LLM bulk classify** (if configured) — all unmatched rows are sent to the LLM in a single call. The LLM sees every description + every candidate account and returns classifications in one shot

```bash
ingest parse MY_BANK statement.csv
# → Staged 50 row(s) from MY_BANK (session 1): 45 classified, 3 pending, 2 errors, 0 duplicates
```

### 4. `ingest review` — Inspect the session

Non-interactive table view. Filter by status or show only unmatched rows.

```bash
ingest review --session 1
ingest review --session 1 --unmatched        # only rows without an account
ingest review --session 1 --status CLASSIFIED
```

### 5. `ingest edit` — Fix individual rows

Correct any field before applying:

```bash
ingest edit --session 1 --row 0 --field account --value "Groceries"
ingest edit --session 1 --row 3 --field description --value "Electricity bill"
ingest edit --session 1 --row 5 --field status --value DISCARDED
```

### 6. `ingest apply` — Commit to the ledger

Creates transactions and **auto-generates regex rules** for future imports:

```bash
ingest apply --session 1
# → Applied 47 row(s) to ledger (session 1). 0 skipped, 3 remaining pending, 0 discarded.
```

Each successfully applied row creates a rule like `(?i).*<keyword>.*` at priority 100. The keyword is the description with variable parts (ref numbers, dates, amounts) stripped — so future statements from the same source match immediately without needing the LLM.

### 7. `ingest discard` — Remove rows or cancel a session

```bash
ingest discard --session 1 --row 3     # discard a single row
ingest discard --session 1              # cancel the entire session
```

---

## Ingestion Rules

Rules are regex patterns that match transaction descriptions to target accounts. They run before the LLM and are the fastest path to classification.

### How auto-rules are created

When `ingest apply` runs, each classified row generates a rule automatically. The description is cleaned — ref numbers, dates, amounts, and standalone numbers are stripped — leaving a stable keyword that matches future statements:

| Raw description | Generated pattern |
|----------------|-------------------|
| `TT REF: LN12345678901234 AED 500 SUPERMARKET ABC DEF LLC ...` | `(?i).*SUPERMARKET ABC DEF LLC.*` |
| `CARD NO.1234********5678 Coffee Shop Downtown:AE 978254...` | `(?i).*Coffee Shop Downtown.*` |
| `MOBILE BANKING TRANSFER TO AE123456789012345678901 RefNo:- ABC123DEF456` | `(?i).*MOBILE BANKING TRANSFER TO.*` |
| `SALARY TRANSFER FROM EMPLOYER NAME HERE ...` | `(?i).*SALARY TRANSFER FROM EMPLOYER.*` |

### Manual rules

Add custom rules at any time. Higher priority wins — the system uses `priority DESC, id ASC` ordering.

```bash
rule add "(?i).*Supermarket.*" 12        # anything with "Supermarket" → account 12
rule add "(?i).*(Salary|Payroll).*" 7    # salary patterns → account 7
```

Default auto-rules use priority 100. Manual rules should use higher numbers to take precedence.

### Amount direction

The parser correctly handles both amount formats:
- **Signed amounts** — positive = inflow, negative = outflow
- **Debit/credit columns** — debit values are negated (outflow), credit values are positive (inflow)

At apply time, the amount sign determines the direction:
- **Positive amount** → money flows from the classified account **to** your anchored account (inflow)
- **Negative amount** → money flows from your anchored account **to** the classified account (outflow)

---

## Walkthrough: Import a Bank Statement End-to-End

```bash
# 0. Register assets and accounts first
asset register "US Dollar" USD --category cash
account init --name "Checking" --asset USD --balance 5000
account create --name "Groceries" --parent-account-id 4    # EXPENSE
account create --name "Salary" --parent-account-id 4       # INCOME

# 1. Convert if needed (PDF → CSV)
convert ~/Downloads/bank-statement.pdf

# 2. Register the source system
source add --name "My Bank" --code MY_BANK --file ~/Downloads/statement.csv

# 3. Parse + auto-classify
ingest parse MY_BANK ~/Downloads/statement.csv

# 4. Review what happened
ingest review --session 1
ingest review --session 1 --unmatched

# 5. Fix anything the LLM got wrong
ingest edit --session 1 --row 4 --field account --value "Groceries"
ingest edit --session 1 --row 7 --field status --value DISCARDED

# 6. Commit to ledger (auto-creates rules for next time)
ingest apply --session 1

# 7. Verify the ledger
transaction list --from-account-id 2
report nw USD
```

---

## Command Reference

### Assets

| Command | Description |
|---------|-------------|
| `cat-list` | List all asset categories |
| `asset list` | List all registered assets |
| `asset register <name> <symbol> [--category <cat>]` | Register a new asset |
| `asset fetch <category> <exchange>` | Sync instruments from market data provider |

### Accounts

| Command | Description |
|---------|-------------|
| `account list` | Display the colour-coded account tree |
| `account create --name <n> [--parent-account-id <id>]` | Create an account (interactive) |
| `account init --name <n> --asset <symbol> --balance <amt>` | One-shot: account + opening balance |
| `account delete <id>` | Delete an account |
| `account find [<keyword>]` | Search accounts |

### Transactions

| Command | Description |
|---------|-------------|
| `transaction add -F <from> -T <to> -a <amount>` | Record a transaction |
| `transaction list [-T <type>] [-f <from>] [-t <to>]` | List/filter transactions |
| `transaction report [-T <type>]` | Export to CSV |

### Ingestion

| Command | Description |
|---------|-------------|
| `convert --input <path> [--output <path>]` | Convert PDF/image/XLS to CSV via LLM |
| `source add --name <n> --code <c> [--file <path>]` | Register a source system |
| `source list` | List all source systems |
| `source show --code <c>` | Show source system details |
| `source remove --code <c>` | Remove a source system |
| `source update-account --code <c> --account <id>` | Set the anchored account |
| `source update-format --code <c> --file <path>` | Re-detect CSV format via LLM |
| `ingest parse --source <code> --file-path <path>` | Parse CSV + auto-classify (rules → LLM bulk) |
| `ingest sessions` | List all staging sessions |
| `ingest review --session <id> [--status <s>] [--unmatched]` | View staged rows |
| `ingest edit --session <id> --row <n> --field <f> --value <v>` | Edit a staged row |
| `ingest apply --session <id>` | Commit to ledger + auto-create rules |
| `ingest discard --session <id> [--row <n>]` | Discard session or row |
| `rule add <pattern> <target-id>` | Add a regex rule |

### Market Quotes

| Command | Description |
|---------|-------------|
| `quote fetch <base> <quote>` | Auto-fetch price from Yahoo Finance |
| `quote set <base> <quote> --price <v> [--date <d>]` | Store a manual quote |
| `quote get <base> <quote>` | Retrieve stored quotes |
| `quote list` | List latest quote for every pair |
| `quote refresh` | Update every stored pair; failed pairs are reported, not fatal |

### System & Reports

| Command | Description |
|---------|-------------|
| `hmc init --account <name> --balance <amt>` | Post an opening balance |
| `hmc reconcile --account <name> --actual <amt>` | Fix a balance discrepancy |
| `hmc db backup [--output <dir>]` | Backup the database |
| `report nw [<currency>]` | Net worth report (default: EGP) |
| `report data-integrity` | Ledger health check |
| `audit account [<id-or-name>]` | Audit a single account |
| `audit trail` | Full ledger audit |
| `info` | Show the financial data pipeline guide |

---

## Project Structure

```
src/main/java/org/hameed/hameedmoneycli/
├── commands/        # Spring Shell command definitions
├── config/          # Spring beans, RestClient, strategy wiring
├── constants/       # Command help strings, LLM prompts
├── enums/           # AccountType, AssetCategory, etc.
├── ingestion/       # CSV parser, amount parser, ingestion utilities
├── model/
│   ├── dto/         # Request/response DTOs
│   └── entity/      # JPA entities
├── proxy/           # LLM proxy, market data providers
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic layer
└── util/            # Shared utilities, date helpers
```

### Database tables

| Table | Role |
|-------|------|
| `assets` | Financial instruments and currencies |
| `accounts` | Hierarchical account tree |
| `source_systems` | Data import sources with format configs |
| `transactions` | Double-entry ledger |
| `ingestion_rules` | Regex patterns for auto-classification |
| `market_quotes` | FX rates and security prices |
| `ingestion_staging_sessions` | Ingest session state |
| `ingested_staged_transactions` | Staged rows awaiting apply |
