package org.hameed.hameedmoneycli.commands;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class WelcomeBanner {

    @PostConstruct
    public void scheduleBanner() {
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            System.out.println();
            System.out.println("  \033[36m╔══════════════════════════════════════════════════╗");
            System.out.println("  \033[36m║              HameedMoneyCLI                      ║");
            System.out.println("  \033[36m║         Your Personal Finance Manager            ║");
            System.out.println("  \033[36m╚══════════════════════════════════════════════════╝\033[0m");
            System.out.println();
            System.out.println("  \033[33mThe financial data pipeline:\033[0m");
            System.out.println();
            System.out.println("  \033[32m0.\033[0m \033[37mStart fresh\033[0m");
            System.out.println("     Seed data provides 14 currencies (EGP, USD, EUR, AED, ...)");
            System.out.println("     and a default account tree (EGP:Food, AED:Food, etc.).");
            System.out.println("     Run  \033[37maccount list\033[0m  to see what's there.");
            System.out.println();
            System.out.println("  \033[32m1.\033[0m \033[37mAssets\033[0m  \u2014  define what you can hold (currencies, stocks, crypto, ...)");
            System.out.println("     \033[2m\u2022\033[0m \033[37mcat-list\033[0m                \u2014  see available categories (CASH, STOCK, ETF, ...)");
            System.out.println("     \033[2m\u2022\033[0m \033[37masset fetch stock EGX\033[0m  \u2014  fetch securities from an exchange");
            System.out.println("     \033[2m\u2022\033[0m \033[37masset register\033[0m           \u2014  manually register what cannot be fetched");
            System.out.println("     \033[2m\u2022\033[0m \033[37masset list\033[0m              \u2014  list all registered assets");
            System.out.println();
            System.out.println("  \033[32m2.\033[0m \033[37mMarket quotes\033[0m  \u2014  prices that feed into net worth calculations");
            System.out.println("     \033[2m\u2022\033[0m \033[37mquote fetch AAPL USD\033[0m   \u2014  fetch a live quote from Yahoo Finance");
            System.out.println("     \033[2m\u2022\033[0m \033[37mquote set USD EGP --price 48.5\033[0m  \u2014  set a quote manually");
            System.out.println("     \033[2m\u2022\033[0m \033[37mquote list\033[0m             \u2014  see all stored quotes");
            System.out.println();
            System.out.println("  \033[32m3.\033[0m \033[37mAccounts\033[0m  \u2014  build the tree (folders \u2192 leaf accounts with assets)");
            System.out.println("     \033[2m\u2022\033[0m \033[37maccount create\033[0m           \u2014  interactive: pick type, asset, parent");
            System.out.println("     \033[2m\u2022\033[0m \033[37maccount init --name \"Wallet\" --asset EGP --balance 1000\033[0m");
            System.out.println("     \033[90m        one-shot: create account + post opening balance\033[0m");
            System.out.println("     \033[2m\u2022\033[0m \033[37maccount list\033[0m             \u2014  view the tree");
            System.out.println("     \033[2m\u2022\033[0m \033[37maccount delete <id>\033[0m      \u2014  remove an account");
            System.out.println();
            System.out.println("  \033[32m4.\033[0m \033[37mTransactions\033[0m  \u2014  record money movement between accounts");
            System.out.println("     \033[2m\u2022\033[0m \033[37mtransaction add --from <id> --to <id> --amount 50 --desc \"Lunch\"\033[0m");
            System.out.println("     \033[2m\u2022\033[0m \033[37mingest HSBC_APP /path/to/statement.csv\033[0m  \u2014  import bank exports");
            System.out.println("     \033[2m\u2022\033[0m \033[37mtransaction list\033[0m         \u2014  view all transactions (with optional filters)");
            System.out.println("     \033[2m\u2022\033[0m \033[37mtransaction report\033[0m       \u2014  export to CSV");
            System.out.println();
            System.out.println("  \033[32m5.\033[0m \033[37mReports & Audit\033[0m  \u2014  understand your financial picture");
            System.out.println("     \033[2m\u2022\033[0m \033[37mreport nw EGP\033[0m           \u2014  net worth statement (balance sheet)");
            System.out.println("     \033[2m\u2022\033[0m \033[37mreport data-integrity\033[0m   \u2014  check ledger integrity (debits vs credits)");
            System.out.println("     \033[2m\u2022\033[0m \033[37maudit account <id>\033[0m      \u2014  verify a specific account's balance");
            System.out.println("     \033[2m\u2022\033[0m \033[37maudit trail\033[0m             \u2014  full ledger audit");
            System.out.println();
            System.out.println("  \033[32m6.\033[0m \033[37mAdjustments\033[0m  \u2014  correct opening balances & reconcile");
            System.out.println("     \033[2m\u2022\033[0m \033[37mhmc init --account \"EGP:Wallet\" --balance 5000\033[0m");
            System.out.println("     \033[2m\u2022\033[0m \033[37mhmc reconcile --account \"EGP:Wallet\" --actual 4990\033[0m");
            System.out.println();
            System.out.println("  \033[33mTip:\033[0m account names are auto-prefixed with their asset symbol.");
            System.out.println("       Type  \033[37mhelp\033[0m  for all commands,  \033[37mhelp <command>\033[0m  for details.");
            System.out.println();
        });
    }
}
