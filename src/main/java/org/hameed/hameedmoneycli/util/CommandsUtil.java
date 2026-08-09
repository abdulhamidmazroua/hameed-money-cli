package org.hameed.hameedmoneycli.util;

import org.hameed.hameedmoneycli.model.entity.Account;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.ExitStatus;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.availability.AvailabilityProvider;
import org.springframework.shell.core.command.exit.ExitStatusExceptionMapper;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

public final class CommandsUtil {

    private CommandsUtil() {
    }

    public static String guidelines() {
        return """
                
                  \033[33mThe financial data pipeline:\033[0m
                
                  \033[32m0.\033[0m \033[37mStart fresh\033[0m
                     Seed data provides 14 currencies (EGP, USD, EUR, AED, ...)
                     and a default account tree (EGP:Food, AED:Food, etc.).
                     Run  \033[37maccount list\033[0m  to see what's there.
                
                  \033[32m1.\033[0m \033[37mAssets\033[0m  \u2014  define what you can hold (currencies, stocks, crypto, ...)
                     \033[2m\u2022\033[0m \033[37mcat-list\033[0m                \u2014  see available categories (CASH, STOCK, ETF, ...)
                     \033[2m\u2022\033[0m \033[37masset fetch stock EGX\033[0m  \u2014  fetch securities from an exchange
                     \033[2m\u2022\033[0m \033[37masset register\033[0m           \u2014  manually register what cannot be fetched
                     \033[2m\u2022\033[0m \033[37masset list\033[0m              \u2014  list all registered assets
                
                  \033[32m2.\033[0m \033[37mMarket quotes\033[0m  \u2014  prices that feed into net worth calculations
                     \033[2m\u2022\033[0m \033[37mquote fetch AAPL USD\033[0m   \u2014  fetch a live quote from Yahoo Finance
                     \033[2m\u2022\033[0m \033[37mquote refresh\033[0m        \u2014  update every stored pair (failures reported, not fatal)
                     \033[2m\u2022\033[0m \033[37mquote set USD EGP --price 48.5\033[0m  \u2014  set a quote manually
                     \033[2m\u2022\033[0m \033[37mquote list\033[0m             \u2014  see all stored quotes
                
                  \033[32m3.\033[0m \033[37mAccounts\033[0m  \u2014  build the tree (folders \u2192 leaf accounts with assets)
                     \033[2m\u2022\033[0m \033[37maccount create\033[0m           \u2014  interactive: pick type, asset, parent
                     \033[2m\u2022\033[0m \033[37maccount init --name "Wallet" --asset EGP --balance 1000\033[0m
                     \033[90m        one-shot: create account + post opening balance\033[0m
                     \033[2m\u2022\033[0m \033[37maccount list\033[0m             \u2014  view the tree
                     \033[2m\u2022\033[0m \033[37maccount delete <id>\033[0m      \u2014  remove an account
                
                  \033[32m4.\033[0m \033[37mTransactions\033[0m  \u2014  record money movement between accounts
                     \033[2m\u2022\033[0m \033[37mtransaction add --from <id> --to <id> --amount 50 --desc "Lunch"\033[0m
                     \033[2m\u2022\033[0m \033[37mconvert /path/to/statement.pdf\033[0m  \u2014  convert PDF/image/XLS to CSV
                     \033[2m\u2022\033[0m \033[37msource add --name ENBD --code ENBD_APP --file data.csv\033[0m  \u2014  register source
                     \033[2m\u2022\033[0m \033[37mingest parse --source ENBD_APP --file-path data.csv\033[0m  \u2014  parse + auto-classify (rules/LLM)
                     \033[2m\u2022\033[0m \033[37mingest review --session 1\033[0m  \u2014  view staged rows
                     \033[2m\u2022\033[0m \033[37mingest apply --session 1\033[0m  \u2014  commit to ledger
                     \033[2m\u2022\033[0m \033[37mingest discard --session 1 --row 0\033[0m  \u2014  discard row or session
                     \033[2m\u2022\033[0m \033[37mtransaction list\033[0m         \u2014  view all transactions
                
                  \033[32m5.\033[0m \033[37mReports & Audit\033[0m  \u2014  understand your financial picture
                     \033[2m\u2022\033[0m \033[37mreport nw EGP\033[0m           \u2014  net worth statement (balance sheet)
                     \033[2m\u2022\033[0m \033[37mreport data-integrity\033[0m   \u2014  check ledger integrity (debits vs credits)
                     \033[2m\u2022\033[0m \033[37maudit account <id>\033[0m      \u2014  verify a specific account's balance
                     \033[2m\u2022\033[0m \033[37maudit trail\033[0m             \u2014  full ledger audit
                
                  \033[32m6.\033[0m \033[37mAdjustments\033[0m  \u2014  correct opening balances & reconcile
                     \033[2m\u2022\033[0m \033[37mhmc init --account "EGP:Wallet" --balance 5000\033[0m
                     \033[2m\u2022\033[0m \033[37mhmc reconcile --account "EGP:Wallet" --actual 4990\033[0m
                
                  \033[33mTip:\033[0m account names are auto-prefixed with their asset symbol.
                       Type  \033[37mhelp\033[0m  for all commands,  \033[37mhelp <command>\033[0m  for details.
                       \033[2mSource add / update-format accept CSV only.\033[0m Use \033[37mconvert\033[0m for other formats.
                """;
    }

    public static String manual() {
        return """
            \033[36m\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2555
            \033[36m\u2551                   HameedMoneyCLI \u2014 System Manual                 \u2551
            \033[36m\u255A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255D\033[0m

            \033[33mOVERVIEW\033[0m
            \033[37mHameedMoneyCLI\033[0m is a double-entry personal finance ledger for the command line.
            It uses \033[37mdouble-entry accounting\033[0m: every transaction moves money from one account
            to another. Balances are \033[37mnever stored\033[0m \u2014 they are computed from the transaction log
            on demand. This guarantees that the ledger is always internally consistent.

            Architecture:
              \033[2m\u2022\033[0m \033[37mSpring Boot 4 + Spring Shell 4\033[0m \u2014 CLI framework
              \033[2m\u2022\033[0m \033[37mSQLite\033[0m via JDBC \u2014 single-file database at \033[2m~/.hmc/hmc.db\033[0m
              \033[2m\u2022\033[0m \033[37mGraalVM 25\033[0m native binary \u2014 fast startup, no JVM overhead


            \033[33mCONFIGURATION\033[0m
            The config file lives at \033[2m~/.hmc/config.json\033[0m and is created automatically on
            first launch. Full example:

              {
                "marketDataProvider": "eodhd",
                "eodhd": { "apiKey": "YOUR_EODHD_API_KEY" },
                "llm": {
                  "provider": "ollama",
                  "model": "llama3",
                  "baseUrl": "http://localhost:11434/api/chat",
                  "apiKey": "",
                  "classifyPrompt": "Optional custom classification prompt"
                }
              }

            LLM Providers:
              Provider    Default Model         baseUrl (used as-is, model embedded in URL)
              \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500    \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500  \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500
              ollama      llama3                http://localhost:11434/api/chat
              openai      gpt-4o-mini           https://api.openai.com/v1/chat/completions
              claude      claude-3-haiku        https://api.anthropic.com/v1/messages
              gemini      gemini-2.0-flash      https://generativelanguage.googleapis.com/...

            Without the LLM section:
              \033[2m\u2022\033[0m \033[37mingest parse\033[0m uses regex rules only (no LLM classification)
              \033[2m\u2022\033[0m \033[37mconvert\033[0m will not work
              \033[2m\u2022\033[0m \033[37msource add --file\033[0m / \033[37msource update-format\033[0m cannot auto-detect format


            \033[33mASSETS\033[0m
            An \033[37mAsset\033[0m is anything you can hold a balance in: a currency, a stock, a crypto token,
            a commodity, or a metal. Assets belong to categories:

              Category    Purpose
              \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500    \u2500\u2500\u2500\u2500\u2500\u2500\u2500
              CASH        Fiat currencies (EGP, USD, EUR, AED, ...)
              STOCK       Equities and index funds
              ETF         Exchange-traded funds
              CRYPTO      Cryptocurrencies
              METAL       Precious metals
              COMMODITY   Other commodities

            Commands:
              \033[37mcat-list\033[0m              \u2014  list all available categories
              \033[37masset register\033[0m        \u2014  register a new asset manually
              \033[37masset fetch\033[0m           \u2014  bulk-fetch instruments from a market data provider
              \033[37masset list\033[0m            \u2014  list all registered assets

            On first launch, the system seeds 14 major currencies automatically
            (EGP, USD, EUR, AED, SAR, KWD, QAR, BHD, OMR, GBP, JPY, CHF, CNY, CAD).


            \033[33mACCOUNTS\033[0m
            Accounts form a \033[37mhierarchical tree\033[0m. Folders group leaf accounts; only leaf
            accounts hold an asset and appear in transactions.

            Account types:

              Type        Use case                               Example
              \u2500\u2500\u2500\u2500        \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500                               \u2500\u2500\u2500\u2500\u2500\u2500\u2500
              ASSET       What you own (internal)                Wallet, Bank, Stocks
              LIABILITY   What you owe (internal)                Credit Card, Loan
              INCOME      Where money comes from                  Salary, Dividends
              EXPENSE     Where money goes                        Groceries, Rent
              SYSTEM      Plumbing (opening balances, fixes)     \u2014

            Rules:
              \033[2m\u2022\033[0m ASSET and LIABILITY are \033[37minternal\033[0m accounts \u2014 they represent things you own or owe
              \033[2m\u2022\033[0m INCOME and EXPENSE are \033[37mexternal\033[0m accounts \u2014 money flows between internal and external
              \033[2m\u2022\033[0m Transfers between two external accounts are not allowed

            Naming convention: account names are auto-prefixed with their asset symbol
            (e.g., \033[2mEGP:Wallet\033[0m, \033[2mAED:Food\033[0m).

            Commands:
              \033[37maccount create\033[0m         \u2014  interactive account creation (type, asset, parent)
              \033[37maccount init\033[0m           \u2014  one-shot: create + opening balance
              \033[37maccount list\033[0m           \u2014  view the tree
              \033[37maccount find\033[0m           \u2014  search accounts by name
              \033[37maccount delete\033[0m         \u2014  remove an account


            \033[33mBALANCE DERIVATION\033[0m
            Balances are computed from the transaction log:

              ASSET       \u2211to \u2212 \u2211from
              LIABILITY   \u2211from \u2212 \u2211to
              INCOME      \u2211from \u2212 \u2211to
              EXPENSE     \u2211to \u2212 \u2211from
              SYSTEM      \u2211from \u2212 \u2211to

            A positive balance on an ASSET means you have money; on a LIABILITY it means
            you owe money. An expense with a positive balance means you have spent money.


            \033[33mMARKET QUOTES & MULTI-CURRENCY\033[0m
            Quotes are stored exchange rates or security prices. The system uses a
            \033[37mgraph traversal\033[0m oracle to convert between any two assets:

              \033[2m\u2022\033[0m \033[37mNodes\033[0m are assets (EGP, USD, AAPL, ...)
              \033[2m\u2022\033[0m \033[37mEdges\033[0m are market_quote records (directed prices)
              \033[2m\u2022\033[0m \033[37mIdentity\033[0m: an asset converts to itself at 1:1
              \033[2m\u2022\033[0m \033[37mInverse\033[0m: if USD\u2192EGP = 48.5, then EGP\u2192USD \u2248 1/48.5
              \033[2m\u2022\033[0m BFS finds the shortest conversion path

            Commands:
              \033[37mquote fetch USD EGP\033[0m    \u2014  auto-fetch from Yahoo Finance
              \033[37mquote refresh\033[0m       \u2014  update all stored pairs
              \033[37mquote set USD EGP --price 48.5\033[0m  \u2014  manual quote
              \033[37mquote get USD EGP\033[0m      \u2014  view stored quotes
              \033[37mquote list\033[0m             \u2014  latest quote for every pair


            \033[33mTHE INGESTION PIPELINE\033[0m
            The core workflow automates importing bank statements into the ledger:

              convert \u2192 source add \u2192 ingest parse \u2192 ingest apply
                                          \u2192 auto-creates rules

            \033[37mStep 1: \033[0m\033[37mconvert\033[0m \u2014 Normalise any format to CSV
            Converts PDFs, images, XLS files, or raw text into clean CSV via the LLM.
            Skip if your file is already CSV.

              \033[2m$\033[0m hmc convert ~/Downloads/statement.pdf
              \033[2m$\033[0m hmc convert ~/Downloads/statement.xls --output cleaned.csv

            Requires the LLM section in config and Python OCR packages installed.
            Fallback: manually export to CSV from your banking app.

            \033[37mStep 2: \033[0m\033[37msource add\033[0m \u2014 Register a data source
            Each bank or platform is a source system with a format config.

              \033[2m$\033[0m hmc source add --name "ENBD" --code ENBD_APP --file statement.csv

            The \033[37m--code\033[0m is your unique key for this source. The \033[37m--file\033[0m option triggers
            LLM auto-detection of the CSV column layout (date format, description column,
            amount columns). Without \033[37m--file\033[0m, a bare source is created; detect the format later:

              \033[2m$\033[0m hmc source update-format ADCB_APP --file adcb.csv

            Every source has an \033[37manchored account\033[0m \u2014 the account in your ledger that
            corresponds to this bank account. When you parse, amounts from the statement
            flow to/from this anchor.

            \033[37mStep 3: \033[0m\033[37mingest parse\033[0m \u2014 Parse + auto-classify in one shot
            This is the workhorse. It reads the CSV, validates every row, and classifies
            each transaction:

              1. \033[37mRegex rules\033[0m run first \u2014 if a description matches an existing rule,
                 the account is assigned immediately (no LLM needed)
              2. \033[37mLLM bulk classify\033[0m \u2014 all remaining unmatched rows are sent to the LLM
                 in a single call. The LLM sees every description + every candidate account
                 and returns classifications for all rows at once

              \033[2m$\033[0m hmc ingest parse ENBD_APP statement.csv
              \u2192 Staged 50 row(s) from ENBD_APP (session 1): 45 classified, 3 pending,
                2 errors, 0 duplicates

            \033[37mStep 4: \033[0m\033[37mingest review\033[0m \u2014 Inspect the session
            View what was parsed and classified. Non-interactive table with filtering.

              \033[2m$\033[0m hmc ingest review --session 1
              \033[2m$\033[0m hmc ingest review --session 1 --unmatched
              \033[2m$\033[0m hmc ingest review --session 1 --status CLASSIFIED

            \033[37mStep 5: \033[0m\033[37mingest edit\033[0m \u2014 Fix individual rows
            Correct any field before committing:

              \033[2m$\033[0m hmc ingest edit --session 1 --row 0 --field account --value "EGP:Groceries"
              \033[2m$\033[0m hmc ingest edit --session 1 --row 3 --field description --value "Electricity"
              \033[2m$\033[0m hmc ingest edit --session 1 --row 5 --field status --value DISCARDED

            \033[37mStep 6: \033[0m\033[37mingest apply\033[0m \u2014 Commit to the ledger
            Creates double-entry transactions and \033[37mauto-generates regex rules\033[0m for
            future imports. Rows you edited manually also create rules \u2014 that manual
            correction is the strongest signal.

              \033[2m$\033[0m hmc ingest apply --session 1
              \u2192 Applied 47 row(s) to ledger (session 1). 0 skipped, 3 remaining pending,
                0 discarded.

            Each rule's keyword is the description with variable parts stripped
            (reference numbers, amounts, dates). So next time you import from the same
            source, more rows match immediately without touching the LLM.

            \033[37mStep 7: \033[0m\033[37mingest discard\033[0m \u2014 Remove rows or cancel a session

              \033[2m$\033[0m hmc ingest discard --session 1 --row 3    # discard one row
              \033[2m$\033[0m hmc ingest discard --session 1            # cancel entire session

            \033[37mIdempotency:\033[0m every transaction gets a unique \033[37mexternal_ref_id\033[0m (SHA-256 of
            sourceCode|date|description|amount). Running parse twice on the same file
            produces \033[37mzero duplicates\033[0m.


            \033[33mINGESTION RULES\033[0m
            Rules are regex patterns that match transaction descriptions to accounts.

            \033[37mHow auto-rules work:\033[0m
            When \033[37mingest apply\033[0m runs, each applied row generates a rule. The description
            is cleaned \u2014 reference numbers, dates, amounts are removed \u2014 leaving a stable
            keyword. Example transformations:

              Raw:    "TT REF: LN37100007544482 AED 75877 SUMARA TECHNOLO GY LLCFZ ..."
              Rule:   "(?i).*SUMARA TECHNOLO GY LLCFZ.*"

              Raw:    "MOBILE BANKING TRANSFER TO AE750260001015863475801 RefNo:- ..."
              Rule:   "(?i).*MOBILE BANKING TRANSFER TO.*"

            Auto-rules are created at \033[37mpriority 100\033[0m. Add manual rules at higher
            priority to override:

              \033[2m$\033[0m hmc rule add "(?i).*Salary.*" 7       # priority defaults above auto-rules
              \033[2m$\033[0m hmc rule add "(?i).*Thndr.*" 12

            \033[37mAmount direction:\033[0m
              \033[2m\u2022\033[0m Positive amount \u2192 money flows from the classified account \033[37mto\033[0m your anchor
              \033[2m\u2022\033[0m Negative amount \u2192 money flows from your anchor \033[37mto\033[0m the classified account

            The parser handles both signed amounts and debit/credit column layouts.
            Debits are always negated (outflow).


            \033[33mDIRECT TRANSACTIONS\033[0m
            For one-off entries that are not from an import:

              \033[2m$\033[0m hmc transaction add -F 2 -T 5 -a 50.00 --desc "Lunch at KFC"
              \033[2m$\033[0m hmc transaction list
              \033[2m$\033[0m hmc transaction report

            This bypasses the entire ingestion pipeline and writes directly to the ledger.


            \033[33mREPORTS & AUDIT\033[0m
              \033[37mreport nw EGP\033[0m           \u2014  net worth (balance sheet in your base currency)
              \033[37mreport data-integrity\033[0m   \u2014  verify debits = credits across the ledger
              \033[37maudit account Cash\033[0m      \u2014  verify a single account's computed balance
              \033[37maudit trail\033[0m             \u2014  full transaction trail with running totals


            \033[33mADJUSTMENTS\033[0m
              \033[37mhmc init\033[0m               \u2014  post an opening balance to a new account
              \033[37mhmc reconcile\033[0m          \u2014  fix a discrepancy between computed and actual balance

            Both create SYSTEM transactions that are excluded from net worth reports.


            \033[33mBACKUP\033[0m
              \033[2m$\033[0m hmc db backup                    \u2014  saves to ~/hmc/backups/
              \033[2m$\033[0m hmc db backup --output /path     \u2014  custom output directory

            The database is a single file at \033[2m~/.hmc/hmc.db\033[0m. `hmc db backup` creates
            it to a timestamped SQL file you can restore with sqlite3.


            \033[33mGETTING HELP\033[0m
              \033[37mhelp\033[0m                   \u2014  list every command
              \033[37mhelp <command>\033[0m         \u2014  detailed help for a specific command
              \033[37minfo\033[0m                   \u2014  this manual

            \033[33mTip:\033[0m account names are auto-prefixed with their asset symbol.
                 Type  \033[37mhelp\033[0m  for all commands,  \033[37mhelp <command>\033[0m  for details.
                 \033[2mSource add / update-format accept CSV only.\033[0m Use \033[37mconvert\033[0m for other formats.
            """;
    }

    public static void printAccountTree(List<Account> accounts, Account parent, int level) {
        String indent = "  ".repeat(level);
        String treeConnector = level == 0 ? "" : "\u2514\u2500 ";

        accounts.stream()
                .filter(account -> {
                    if (parent == null) {
                        return account.getParent() == null;
                    }
                    return account.getParent() != null && account.getParent().getId().equals(parent.getId());
                })
                .forEach(account -> {
                    String assetLabel = account.getAsset() == null
                            ? "(folder)"
                            : account.getAsset().getName() + " (" + account.getAsset().getSymbol() + ")";
                    String line = indent + treeConnector +
                            "\u001B[1m\u001B[37m" + account.getName() + "\u001B[0m " +
                            "\u001B[2m\u001B[33m(ID: " + account.getId() + ")\u001B[0m " +
                            "\u001B[95mAsset: " + assetLabel + "\u001B[0m";

                    System.out.println(line);
                    printAccountTree(accounts, account, level + 1);
                });
    }

    public static CommandOption getOption(CommandContext ctx, char shortName, String longName) {
        CommandOption option = ctx.getOptionByLongName(longName);
        return option != null ? option : ctx.getOptionByShortName(shortName);
    }

    public static String getOptionOrDefault(CommandContext ctx, char shortName, String longName, String defaultVal) {
        CommandOption option = getOption(ctx, shortName, longName);
        return isOptionValid(option) ? option.value() : defaultVal;
    }

    public static String getOptionOrError(CommandContext ctx, char shortName, String longName, String errorMessage) {
        CommandOption option = getOption(ctx, shortName, longName);
        if (isOptionValid(option)) {
            return option.value();
        }
        throw new IllegalArgumentException(errorMessage);
    }

    public static boolean isOptionValid(CommandOption option) {
        return option != null && option.value() != null && !option.value().isBlank();
    }

    public static String argOrOption(CommandContext ctx, int argIndex, char shortName, String longName) {
        var args = ctx.parsedInput().arguments();
        if (argIndex < args.size()) {
            return args.get(argIndex).value();
        }
        return getOptionOrDefault(ctx, shortName, longName, null);
    }

    public static String argOrOption(CommandContext ctx, int argIndex, char shortName, String longName, String defaultVal) {
        var args = ctx.parsedInput().arguments();
        if (argIndex < args.size()) {
            return args.get(argIndex).value();
        }
        return getOptionOrDefault(ctx, shortName, longName, defaultVal);
    }

    public static String assetSymbol(Account account) {
        return account.getAsset() == null ? "\u2014" : account.getAsset().getSymbol();
    }

    public static void pagedOutput(String content) {
        String pager = System.getenv("PAGER");
        if (pager == null || pager.isBlank()) {
            if (available("less")) pager = "less -R";
            else if (available("more")) pager = "more";
        }
        if (pager != null) {
            try {
                ProcessBuilder pb = new ProcessBuilder(pager.split(" "));
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                Process process = pb.start();
                try (OutputStream os = process.getOutputStream()) {
                    os.write(content.getBytes());
                    os.flush();
                }
                process.waitFor();
                return;
            } catch (Exception ignored) {
            }
        }
        System.out.print(content);
    }

    private static boolean available(String cmd) {
        try {
            new ProcessBuilder("which", cmd).start().waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static ExitStatusExceptionMapper exceptionMapper() {
        return exception -> {
            if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
                return new ExitStatus(1, exception.getMessage());
            }
            return new ExitStatus(2, "An unexpected error occurred: " + exception.getMessage());
        };
    }

    public static AvailabilityProvider availabilityProvider() {
        return Availability::available;
    }
}
