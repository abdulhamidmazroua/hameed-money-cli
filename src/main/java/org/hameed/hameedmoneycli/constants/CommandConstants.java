package org.hameed.hameedmoneycli.constants;

public final class CommandConstants {

    private CommandConstants() {
    }

    // ─── Shared ────────────────────────────────────────────────────────────────

    public static final String HYBRID_USAGE_WARNING = """
            Note: Mixing options and positional arguments is not recommended — use one style consistently.""";

    // ─── Argument names ────────────────────────────────────────────────────────

    public static final String NAME_ARG = "name";
    public static final String SYMBOL_ARG = "symbol";
    public static final String CATEGORY_ARG = "category";
    public static final String EXCHANGE_ARG = "exchange";
    public static final String DESCRIPTION_ARG = "description";
    public static final String DATE_ARG = "date";
    public static final String BALANCE_ARG = "balance";
    public static final String AMOUNT_ARG = "amount";
    public static final String FROM_AMOUNT_ARG = "from-amount";
    public static final String TO_AMOUNT_ARG = "to-amount";
    public static final String FEE_AMOUNT_ARG = "fee-amount";
    public static final String MIN_AMOUNT_ARG = "min-amount";
    public static final String MAX_AMOUNT_ARG = "max-amount";
    public static final String ACCOUNT_ARG = "account";
    public static final String ACCOUNT_ID_ARG = "account-id";
    public static final String FROM_ACCOUNT_ID_ARG = "from-account-id";
    public static final String TO_ACCOUNT_ID_ARG = "to-account-id";
    public static final String FROM_ACCOUNT_NAME_ARG = "from-account-name";
    public static final String TO_ACCOUNT_NAME_ARG = "to-account-name";
    public static final String ASSET_ARG = "asset";
    public static final String SOURCE_ARG = "source";
    public static final String FILE_PATH_ARG = "file-path";
    public static final String PATTERN_ARG = "pattern";
    public static final String TARGET_ARG = "target";
    public static final String BASE_ARG = "base";
    public static final String QUOTE_ARG = "quote";
    public static final String PRICE_ARG = "price";
    public static final String CURRENCY_ARG = "currency";
    public static final String KEYWORD_ARG = "keyword";
    public static final String TYPE_ARG = "type";
    public static final String ACTUAL_ARG = "actual";
    public static final String OUTPUT_ARG = "output";
    public static final String TRANSACTION_TYPE_ARG = "transaction-type";
    public static final String START_DATE_ARG = "start-date";
    public static final String END_DATE_ARG = "end-date";
    public static final String PARENT_ACCOUNT_ID_ARG = "parent-account-id";
    public static final String PARENT_ACCOUNT_NAME_ARG = "parent-account-name";
    public static final String ID_ARG = "id";
    public static final String TRADABLE_ARG = "tradable";
    public static final String NON_TRADABLE_ARG = "non-tradable";

    // ─── Asset commands ────────────────────────────────────────────────────────

    // cat-list
    public static final String CAT_LIST_COMMAND_DESCRIPTION = "List all asset categories";
    public static final String CAT_LIST_COMMAND_USAGE = """
            Usage:
              cat-list""";
    public static final String CAT_LIST_COMMAND_HELP = CAT_LIST_COMMAND_DESCRIPTION + "\n" + CAT_LIST_COMMAND_USAGE;

    // asset fetch
    public static final String ASSET_FETCH_COMMAND_DESCRIPTION = "Fetch available securities from an exchange";
    public static final String ASSET_FETCH_COMMAND_USAGE = """
            Usage:
              Options:
                asset fetch --category <category> (-c) --exchange <exchange> (-e)
              Positional:
                asset fetch <category> <exchange>
            """ + HYBRID_USAGE_WARNING;
    public static final String ASSET_FETCH_COMMAND_HELP = ASSET_FETCH_COMMAND_DESCRIPTION + "\n" + ASSET_FETCH_COMMAND_USAGE;
    public static final String ASSET_FETCH_CATEGORY_ARG_ERROR = "<category> is missing. Use --category <category> (-c) or pass it as the first positional argument.\n" + ASSET_FETCH_COMMAND_USAGE;
    public static final String ASSET_FETCH_EXCHANGE_ARG_ERROR = "<exchange> is missing. Use --exchange <exchange> (-e) or pass it as the second positional argument.\n" + ASSET_FETCH_COMMAND_USAGE;
    public static final String ASSET_FETCH_UNSUPPORTED_CATEGORY = "Unsupported category: %s. Use stock, etf, or fund.";

    // asset register
    public static final String ASSET_REGISTER_COMMAND_DESCRIPTION = "Register a new asset manually";
    public static final String ASSET_REGISTER_COMMAND_USAGE = """
            Usage:
              Options:
                asset register --name <name> (-n) --symbol <symbol> (-s) [--category <category> (-c)]
              Positional:
                asset register <name> <symbol> [--category <category>]
            """ + HYBRID_USAGE_WARNING;
    public static final String ASSET_REGISTER_COMMAND_HELP = ASSET_REGISTER_COMMAND_DESCRIPTION + "\n" + ASSET_REGISTER_COMMAND_USAGE;
    public static final String ASSET_REGISTER_NAME_ARG_ERROR = "<name> is missing. Use --name <name> (-n) or pass it as the first positional argument.\n" + ASSET_REGISTER_COMMAND_USAGE;
    public static final String ASSET_REGISTER_SYMBOL_ARG_ERROR = "<symbol> is missing. Use --symbol <symbol> (-s) or pass it as the second positional argument.\n" + ASSET_REGISTER_COMMAND_USAGE;

    // asset list
    public static final String ASSET_LIST_COMMAND_DESCRIPTION = "List all registered assets";
    public static final String ASSET_LIST_COMMAND_USAGE = """
            Usage:
              asset list""";
    public static final String ASSET_LIST_COMMAND_HELP = ASSET_LIST_COMMAND_DESCRIPTION + "\n" + ASSET_LIST_COMMAND_USAGE;

    // asset find
    public static final String ASSET_FIND_COMMAND_DESCRIPTION = "Search assets by keyword, category, or tradable status";
    public static final String ASSET_FIND_COMMAND_USAGE = """
            Usage:
              Options:
                asset find [--category <category> (-c)] [--tradable] [--non-tradable]
              Positional:
                asset find [<keyword>]
            """ + HYBRID_USAGE_WARNING;
    public static final String ASSET_FIND_COMMAND_HELP = ASSET_FIND_COMMAND_DESCRIPTION + "\n" + ASSET_FIND_COMMAND_USAGE;

    // ─── Account commands ──────────────────────────────────────────────────────

    // account create
    public static final String ACCOUNT_CREATE_COMMAND_DESCRIPTION = "Create a new account";
    public static final String ACCOUNT_CREATE_COMMAND_USAGE = """
            Usage:
              Options:
                account create --name <name> (-n) [--parent-account-id <id> (-p)] [--parent-account-name <name> (-P)]
              Positional:
                account create <name>
            (Account type and asset are selected interactively when omitted.)
            """ + HYBRID_USAGE_WARNING;
    public static final String ACCOUNT_CREATE_COMMAND_HELP = ACCOUNT_CREATE_COMMAND_DESCRIPTION + "\n" + ACCOUNT_CREATE_COMMAND_USAGE;
    public static final String ACCOUNT_CREATE_NAME_ARG_ERROR = "<name> is missing. Use --name <name> (-n) or pass it as the first positional argument.\n" + ACCOUNT_CREATE_COMMAND_USAGE;

    // account init
    public static final String INIT_ACCOUNT_COMMAND_DESCRIPTION = "Create an account and post its opening balance in one shot";
    public static final String INIT_ACCOUNT_COMMAND_USAGE = """
            Usage:
              Options:
                account init --name <name> (-n) --asset <symbol> (-a) --balance <amount> (-b) [--parent-account-id <id> (-p)]
              Positional:
                account init <name> <asset-symbol> <balance> [--parent-account-id <id>]
            Note: The account name is auto-prefixed with the asset symbol (e.g. EGP:Wallet).
            The asset must already exist — register it first with `asset register`.
            """ + HYBRID_USAGE_WARNING;
    public static final String INIT_ACCOUNT_COMMAND_HELP = INIT_ACCOUNT_COMMAND_DESCRIPTION + "\n" + INIT_ACCOUNT_COMMAND_USAGE;
    public static final String INIT_ACCOUNT_NAME_ARG_ERROR = "<name> is missing. Use --name <name> (-n) or pass it as the first positional argument.\n" + INIT_ACCOUNT_COMMAND_USAGE;
    public static final String INIT_ACCOUNT_ASSET_ARG_ERROR = "<asset> is missing. Use --asset <symbol> (-a) or pass it as the second positional argument. The asset must already exist.\n" + INIT_ACCOUNT_COMMAND_USAGE;
    public static final String INIT_ACCOUNT_BALANCE_ARG_ERROR = "<balance> is missing. Use --balance <amount> (-b) or pass it as the third positional argument.\n" + INIT_ACCOUNT_COMMAND_USAGE;

    // account list
    public static final String ACCOUNT_LIST_COMMAND_DESCRIPTION = "List all accounts in a tree view";
    public static final String ACCOUNT_LIST_COMMAND_USAGE = """
            Usage:
              account list""";
    public static final String ACCOUNT_LIST_COMMAND_HELP = ACCOUNT_LIST_COMMAND_DESCRIPTION + "\n" + ACCOUNT_LIST_COMMAND_USAGE;

    // account find
    public static final String ACCOUNT_FIND_COMMAND_DESCRIPTION = "Search accounts by keyword, type, or asset symbol";
    public static final String ACCOUNT_FIND_COMMAND_USAGE = """
            Usage:
              Options:
                account find [--type <type> (-t)] [--asset <symbol> (-a)]
              Positional:
                account find [<keyword>]
            """ + HYBRID_USAGE_WARNING;
    public static final String ACCOUNT_FIND_COMMAND_HELP = ACCOUNT_FIND_COMMAND_DESCRIPTION + "\n" + ACCOUNT_FIND_COMMAND_USAGE;

    // account delete
    public static final String DELETE_ACCOUNT_COMMAND_DESCRIPTION = "Delete an account";
    public static final String DELETE_ACCOUNT_COMMAND_USAGE = """
            Usage:
              Options:
                account delete [--account-id <id> (-a)]
              Positional:
                account delete [<id>]
            """ + HYBRID_USAGE_WARNING;
    public static final String DELETE_ACCOUNT_COMMAND_HELP = DELETE_ACCOUNT_COMMAND_DESCRIPTION + "\n" + DELETE_ACCOUNT_COMMAND_USAGE;

    // ─── Transaction commands ──────────────────────────────────────────────────

    // transaction add
    public static final String TRANSACTION_ADD_COMMAND_DESCRIPTION = "Record a transaction between two accounts";
    public static final String TRANSACTION_ADD_COMMAND_USAGE = """
            Usage:
              Options:
                transaction add [--amount <n> (-a)] | [--from-amount <n> (-f) --to-amount <n> (-t)]
                             [--fee-amount <n> (-e)] [--date <date> (-d)] [--description <text> (-D)]
                             --from-account-id <id> (-F) | --from-account-name <name> (-N)
                             --to-account-id <id> (-T) | --to-account-name <name> (-M)
              Positional:
                transaction add <from-account> <to-account> [<amount>]
            (<from-account> and <to-account> auto-detect ID vs name.)
            """ + HYBRID_USAGE_WARNING;
    public static final String TRANSACTION_ADD_COMMAND_HELP = TRANSACTION_ADD_COMMAND_DESCRIPTION + "\n" + TRANSACTION_ADD_COMMAND_USAGE;
    public static final String TRANSACTION_ADD_FROM_ACCOUNT_ARG_ERROR = "Either --from-account-id (-F), --from-account-name (-N), or a positional account is required.\n" + TRANSACTION_ADD_COMMAND_USAGE;
    public static final String TRANSACTION_ADD_TO_ACCOUNT_ARG_ERROR = "Either --to-account-id (-T), --to-account-name (-M), or a positional account is required.\n" + TRANSACTION_ADD_COMMAND_USAGE;
    public static final String TRANSACTION_ADD_AMOUNT_ARG_ERROR = "<amount> or <from-amount> is missing. Use --amount <n> (-a), --from-amount <n> (-f), or pass it as the third positional argument.\n" + TRANSACTION_ADD_COMMAND_USAGE;
    public static final String TRANSACTION_ADD_TO_AMOUNT_ARG_ERROR = "<amount> or <to-amount> is missing. Use --amount <n> (-a), --to-amount <n> (-t), or pass it as the third positional argument.\n" + TRANSACTION_ADD_COMMAND_USAGE;

    // transaction list
    public static final String TRANSACTION_LIST_COMMAND_DESCRIPTION = "List transactions with optional filters";
    public static final String TRANSACTION_LIST_COMMAND_USAGE = """
            Usage:
              Options:
                transaction list [--transaction-type <type> (-T)] [--from-account-id <id> (-f)]
                                  [--to-account-id <id> (-t)] [--start-date <date> (-s)]
                                  [--end-date <date> (-e)]
              Positional:
                (All arguments are via options — omitting all lists every transaction.)
            """ + HYBRID_USAGE_WARNING;
    public static final String TRANSACTION_LIST_COMMAND_HELP = TRANSACTION_LIST_COMMAND_DESCRIPTION + "\n" + TRANSACTION_LIST_COMMAND_USAGE;

    // transaction find
    public static final String TRANSACTION_FIND_COMMAND_DESCRIPTION = "Search transactions by keyword, amount range, account, type, or date";
    public static final String TRANSACTION_FIND_COMMAND_USAGE = """
            Usage:
              Options:
                transaction find [--transaction-type <type> (-T)] [--from-account-id <id> (-f)]
                                  [--to-account-id <id> (-t)] [--start-date <date> (-s)]
                                  [--end-date <date> (-e)] [--account-id <id> (-a)]
                                  [--min-amount <n>] [--max-amount <n>]
              Positional:
                transaction find [<keyword>]
            """ + HYBRID_USAGE_WARNING;
    public static final String TRANSACTION_FIND_COMMAND_HELP = TRANSACTION_FIND_COMMAND_DESCRIPTION + "\n" + TRANSACTION_FIND_COMMAND_USAGE;

    // transaction report
    public static final String TRANSACTION_REPORT_COMMAND_DESCRIPTION = "Export transactions to a CSV report";
    public static final String TRANSACTION_REPORT_COMMAND_USAGE = """
            Usage:
              Options:
                transaction report [--transaction-type <type> (-T)] [--from-account-id <id> (-f)]
                                    [--to-account-id <id> (-t)] [--start-date <date> (-s)]
                                    [--end-date <date> (-e)]
              Positional:
                (All arguments are via options — omitting all exports every transaction.)
            """ + HYBRID_USAGE_WARNING;
    public static final String TRANSACTION_REPORT_COMMAND_HELP = TRANSACTION_REPORT_COMMAND_DESCRIPTION + "\n" + TRANSACTION_REPORT_COMMAND_USAGE;

    // ─── Quote commands ────────────────────────────────────────────────────────

    // quote set
    public static final String QUOTE_SET_COMMAND_DESCRIPTION = "Set a market quote manually";
    public static final String QUOTE_SET_COMMAND_USAGE = """
            Usage:
              Options:
                quote set [--base <symbol> (-b)] [--quote <symbol> (-q)] --price <price> (-p) [--date <date> (-d)]
              Positional:
                quote set <base-symbol> <quote-symbol> <price> [--date <date>]
            """ + HYBRID_USAGE_WARNING;
    public static final String QUOTE_SET_COMMAND_HELP = QUOTE_SET_COMMAND_DESCRIPTION + "\n" + QUOTE_SET_COMMAND_USAGE;
    public static final String QUOTE_SET_BASE_ARG_ERROR = "<base> is missing. Use --base <symbol> (-b) or pass it as the first positional argument.\n" + QUOTE_SET_COMMAND_USAGE;
    public static final String QUOTE_SET_QUOTE_ARG_ERROR = "<quote> is missing. Use --quote <symbol> (-q) or pass it as the second positional argument.\n" + QUOTE_SET_COMMAND_USAGE;
    public static final String QUOTE_SET_PRICE_ARG_ERROR = "<price> is missing. Use --price <price> (-p) or pass it as the third positional argument.\n" + QUOTE_SET_COMMAND_USAGE;

    // quote get
    public static final String QUOTE_GET_COMMAND_DESCRIPTION = "Look up a stored market quote";
    public static final String QUOTE_GET_COMMAND_USAGE = """
            Usage:
              Options:
                quote get [--base <symbol> (-b)] [--quote <symbol> (-q)]
              Positional:
                quote get <base-symbol> <quote-symbol>
            """ + HYBRID_USAGE_WARNING;
    public static final String QUOTE_GET_COMMAND_HELP = QUOTE_GET_COMMAND_DESCRIPTION + "\n" + QUOTE_GET_COMMAND_USAGE;
    public static final String QUOTE_GET_BASE_ARG_ERROR = "<base> is missing. Use --base <symbol> (-b) or pass it as the first positional argument.\n" + QUOTE_GET_COMMAND_USAGE;
    public static final String QUOTE_GET_QUOTE_ARG_ERROR = "<quote> is missing. Use --quote <symbol> (-q) or pass it as the second positional argument.\n" + QUOTE_GET_COMMAND_USAGE;

    // quote fetch
    public static final String QUOTE_FETCH_COMMAND_DESCRIPTION = "Fetch the latest quote from Yahoo Finance and save it";
    public static final String QUOTE_FETCH_COMMAND_USAGE = """
            Usage:
              Options:
                quote fetch [--base <symbol> (-b)] [--quote <symbol> (-q)]
              Positional:
                quote fetch <base-symbol> <quote-symbol>
            """ + HYBRID_USAGE_WARNING;
    public static final String QUOTE_FETCH_COMMAND_HELP = QUOTE_FETCH_COMMAND_DESCRIPTION + "\n" + QUOTE_FETCH_COMMAND_USAGE;
    public static final String QUOTE_FETCH_BASE_ARG_ERROR = "<base> is missing. Use --base <symbol> (-b) or pass it as the first positional argument.\n" + QUOTE_FETCH_COMMAND_USAGE;
    public static final String QUOTE_FETCH_QUOTE_ARG_ERROR = "<quote> is missing. Use --quote <symbol> (-q) or pass it as the second positional argument.\n" + QUOTE_FETCH_COMMAND_USAGE;

    // quote list
    public static final String QUOTE_LIST_COMMAND_DESCRIPTION = "List all stored market quotes";
    public static final String QUOTE_LIST_COMMAND_USAGE = """
            Usage:
              quote list""";
    public static final String QUOTE_LIST_COMMAND_HELP = QUOTE_LIST_COMMAND_DESCRIPTION + "\n" + QUOTE_LIST_COMMAND_USAGE;

    // ─── Report commands ───────────────────────────────────────────────────────

    // report nw
    public static final String REPORT_NW_COMMAND_DESCRIPTION = "Net worth statement valued in a currency";
    public static final String REPORT_NW_COMMAND_USAGE = """
            Usage:
              Options:
                report nw [--currency <currency> (-c)]
              Positional:
                report nw [<currency>]
            """ + HYBRID_USAGE_WARNING;
    public static final String REPORT_NW_COMMAND_HELP = REPORT_NW_COMMAND_DESCRIPTION + "\n" + REPORT_NW_COMMAND_USAGE;

    // report data-integrity
    public static final String REPORT_DATA_INTEGRITY_COMMAND_DESCRIPTION = "Ledger data integrity report";
    public static final String REPORT_DATA_INTEGRITY_COMMAND_USAGE = """
            Usage:
              report data-integrity""";
    public static final String REPORT_DATA_INTEGRITY_COMMAND_HELP = REPORT_DATA_INTEGRITY_COMMAND_DESCRIPTION + "\n" + REPORT_DATA_INTEGRITY_COMMAND_USAGE;

    // audit account
    public static final String AUDIT_ACCOUNT_COMMAND_DESCRIPTION = "Verify an account's computed balance";
    public static final String AUDIT_ACCOUNT_COMMAND_USAGE = """
            Usage:
              Options:
                audit account [--id <id> (-i)] [--name <name> (-n)]
              Positional:
                audit account [<id>] or audit account [<name>]
            """ + HYBRID_USAGE_WARNING;
    public static final String AUDIT_ACCOUNT_COMMAND_HELP = AUDIT_ACCOUNT_COMMAND_DESCRIPTION + "\n" + AUDIT_ACCOUNT_COMMAND_USAGE;

    // audit trail
    public static final String AUDIT_TRAIL_COMMAND_DESCRIPTION = "Full ledger data integrity check";
    public static final String AUDIT_TRAIL_COMMAND_USAGE = """
            Usage:
              audit trail""";
    public static final String AUDIT_TRAIL_COMMAND_HELP = AUDIT_TRAIL_COMMAND_DESCRIPTION + "\n" + AUDIT_TRAIL_COMMAND_USAGE;

    // ─── System commands ───────────────────────────────────────────────────────

    // ingest
    public static final String INGEST_COMMAND_DESCRIPTION = "Import transactions from a CSV file";
    public static final String INGEST_COMMAND_USAGE = """
            Usage:
              Options:
                ingest --source <source> (-s) --file-path <path> (-f)
              Positional:
                ingest <source> <file-path>
            """ + HYBRID_USAGE_WARNING;
    public static final String INGEST_COMMAND_HELP = INGEST_COMMAND_DESCRIPTION + "\n" + INGEST_COMMAND_USAGE;
    public static final String INGEST_SOURCE_ARG_ERROR = "<source> is missing. Use --source <source> (-s) or pass it as the first positional argument.\n" + INGEST_COMMAND_USAGE;
    public static final String INGEST_FILE_PATH_ARG_ERROR = "<file-path> is missing. Use --file-path <path> (-f) or pass it as the second positional argument.\n" + INGEST_COMMAND_USAGE;
    public static final String INGEST_FAILED = "Ingestion failed: %s";

    // rule add
    public static final String RULE_ADD_COMMAND_DESCRIPTION = "Add a transaction ingestion rule";
    public static final String RULE_ADD_COMMAND_USAGE = """
            Usage:
              Options:
                rule add --pattern <regex> (-p) --target <account-id> (-t)
              Positional:
                rule add <pattern> <target-id>
            """ + HYBRID_USAGE_WARNING;
    public static final String RULE_ADD_COMMAND_HELP = RULE_ADD_COMMAND_DESCRIPTION + "\n" + RULE_ADD_COMMAND_USAGE;
    public static final String RULE_ADD_PATTERN_ARG_ERROR = "<pattern> is missing. Use --pattern <regex> (-p) or pass it as the first positional argument.\n" + RULE_ADD_COMMAND_USAGE;
    public static final String RULE_ADD_TARGET_ARG_ERROR = "<target> is missing. Use --target <account-id> (-t) or pass it as the second positional argument.\n" + RULE_ADD_COMMAND_USAGE;

    // hmc init
    public static final String HMC_INIT_COMMAND_DESCRIPTION = "Post an opening balance to an account";
    public static final String HMC_INIT_COMMAND_USAGE = """
            Usage:
              Options:
                hmc init [--account <name> (-a)] [--account-id <id> (-i)] --balance <amount> (-b)
              Positional:
                hmc init [<account-name>] [<balance>]
            """ + HYBRID_USAGE_WARNING;
    public static final String HMC_INIT_COMMAND_HELP = HMC_INIT_COMMAND_DESCRIPTION + "\n" + HMC_INIT_COMMAND_USAGE;
    public static final String HMC_INIT_BALANCE_ARG_ERROR = "<balance> is missing. Use --balance <amount> (-b) or pass it as the second positional argument.\n" + HMC_INIT_COMMAND_USAGE;
    public static final String HMC_INIT_ACCOUNT_NOT_FOUND = "Either --account <name>, --account-id <id>, or a positional account name is required.";

    // hmc reconcile
    public static final String HMC_RECONCILE_COMMAND_DESCRIPTION = "Reconcile an account to its actual balance";
    public static final String HMC_RECONCILE_COMMAND_USAGE = """
            Usage:
              Options:
                hmc reconcile [--account <name> (-a)] [--account-id <id> (-i)] --actual <amount> (-c)
              Positional:
                hmc reconcile [<account-name>] [<actual-amount>]
            """ + HYBRID_USAGE_WARNING;
    public static final String HMC_RECONCILE_COMMAND_HELP = HMC_RECONCILE_COMMAND_DESCRIPTION + "\n" + HMC_RECONCILE_COMMAND_USAGE;
    public static final String HMC_RECONCILE_ACTUAL_ARG_ERROR = "<actual> is missing. Use --actual <amount> (-c) or pass it as the second positional argument.\n" + HMC_RECONCILE_COMMAND_USAGE;
    public static final String HMC_RECONCILE_ACCOUNT_NOT_FOUND = "Either --account <name>, --account-id <id>, or a positional account name is required.";

    // hmc db backup
    public static final String HMC_DB_BACKUP_COMMAND_DESCRIPTION = "Backup the database via pg_dump";
    public static final String HMC_DB_BACKUP_COMMAND_USAGE = """
            Usage:
              Options:
                hmc db backup [--output <dir> (-o)]
              Positional:
                hmc db backup""";
    public static final String HMC_DB_BACKUP_COMMAND_HELP = HMC_DB_BACKUP_COMMAND_DESCRIPTION + "\n" + HMC_DB_BACKUP_COMMAND_USAGE;

    // info
    public static final String INFO_COMMAND_DESCRIPTION = "Show the financial data pipeline guide";
    public static final String INFO_COMMAND_USAGE = """
            Usage:
              info""";
    public static final String INFO_COMMAND_HELP = INFO_COMMAND_DESCRIPTION + "\n" + INFO_COMMAND_USAGE;
}
