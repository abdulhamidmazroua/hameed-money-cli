package org.hameed.hameedmoneycli.util;

import org.hameed.hameedmoneycli.model.entity.Account;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.ExitStatus;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.availability.AvailabilityProvider;
import org.springframework.shell.core.command.exit.ExitStatusExceptionMapper;

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
                     \033[2m\u2022\033[0m \033[37mingest HSBC_APP /path/to/statement.csv\033[0m  \u2014  import bank exports
                     \033[2m\u2022\033[0m \033[37mtransaction list\033[0m         \u2014  view all transactions (with optional filters)
                     \033[2m\u2022\033[0m \033[37mtransaction report\033[0m       \u2014  export to CSV
                
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
