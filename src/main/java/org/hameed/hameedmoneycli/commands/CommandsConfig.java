package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AccountType;
import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.enums.SourceSystemCode;
import org.hameed.hameedmoneycli.enums.TransactionType;
import org.hameed.hameedmoneycli.model.dto.AccountCreateDto;
import org.hameed.hameedmoneycli.model.dto.AssetCreateDto;
import org.hameed.hameedmoneycli.model.dto.TransactionCreateDto;
import org.hameed.hameedmoneycli.model.dto.TransactionFilter;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.service.AccountService;
import org.hameed.hameedmoneycli.service.AssetService;
import org.hameed.hameedmoneycli.service.IngestionService;
import org.hameed.hameedmoneycli.service.TransactionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandArgument;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class CommandsConfig {

    private final ComponentFlow.Builder componentFlowBuilder;
    private final AssetService assetService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final IngestionService ingestionService;

    // Assets and Accounts
    @Bean
    public Command registerAsset() {
        return Command.builder()
                .name("asset register")
                .description("Register a new asset")
                .help("Register a new asset. Usage: `asset register --name \"Commercial International Bank\" --symbol COMI.CA`")
                .options(CommandOption.with()
                            .shortName('n')
                            .longName("name")
                            .required(true)
                            .type(String.class)
                            .build(),
                        CommandOption.with()
                            .shortName('s')
                            .longName("symbol")
                            .required(true)
                            .type(String.class)
                            .build())
//                .exitStatusExceptionMapper(exceptionMapper())
//                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    String usage = "Usage: `asset register --name \"Commercial International Bank\" --symbol COMI.CA`";
                    String assetName = getOption(ctx, "name", "<name> option is missing. \n" + usage);
                    String symbol = getOption(ctx, "symbol", "<name> option is missing. \n" + usage);

                    ComponentFlow.ComponentFlowResult assetCategoryResult = componentFlowBuilder.clone().reset()
                            .withSingleItemSelector("assetCategory")
                            .name("Asset Category: ")
                            .selectItems(List.of(AssetCategory.values()).stream()
                                    .map(category -> SelectItem.of(category.toString(), category.toString()))
                                    .toList(
                            )).and().build().run();

                    String assetCategory = assetCategoryResult.getContext().get("assetCategory", String.class);
                    assetService.createAsset(new AssetCreateDto(assetName, symbol, AssetCategory.valueOf(assetCategory), AssetCategory.valueOf(assetCategory).isTradable()));
                });
    }

    public Command createAccount() {
        return Command.builder()
                .name("account create")
                .description("Create a new account")
                .help("Create a new account. Usage: `account create --name \"Cash Account\" --parent-account-id 3 `")
                .options(CommandOption.with()
                                .shortName('n')
                                .longName("name")
                                .required(true)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('p')
                                .longName("parent-account-id")
                                .required(false)
                                .type(String.class)
                                .build())
                .execute(ctx -> {
                        String usage = "Usage: `account create --name \"Cash Account\" --parent-account-id 3 `";
                        String accountName = getOption(ctx, "name", "<name> option is missing. \n" + usage);
                        String parentAccountId = ctx.getOptionByLongName("parent-account-id").value();

                        // selecting the account type (master type) for the account
                        ComponentFlow.ComponentFlowResult accountTypeResult = componentFlowBuilder.clone().reset()
                                .withSingleItemSelector("accountType")
                                .name("Account Type: ")
                                .selectItems(List.of(AccountType.values()).stream()
                                        .map(accountType -> SelectItem.of(accountType.toString(), accountType.toString()))
                                        .toList())
                                .and().build().run();

                        List<SelectItem> assetChoices = new ArrayList<>();
                        assetChoices.add(SelectItem.of("(Folder — organizational only, no asset / not for postings)", ""));
                        assetChoices.addAll(assetService.getAllAssets().stream()
                                .map(asset -> SelectItem.of(asset.getName() + " (" + asset.getSymbol() + ")", asset.getId().toString()))
                                .toList());

                        ComponentFlow.ComponentFlowResult assetResult = componentFlowBuilder.clone().reset()
                            .withSingleItemSelector("assetId")
                            .name("Leaf: pick an asset. Parent folder: choose the first option.")
                            .selectItems(assetChoices)
                            .and().build().run();

                        String accountType = accountTypeResult.getContext().get("accountType", String.class);
                        String assetId = assetResult.getContext().get("assetId", String.class);
                        Long assetIdLong = (assetId != null && !assetId.isBlank()) ? Long.valueOf(assetId) : null;
                        accountService.createAccount(new AccountCreateDto(
                                accountName,
                                AccountType.valueOf(accountType),
                                parentAccountId != null && !parentAccountId.isBlank() ? Long.valueOf(parentAccountId) : null,
                                assetIdLong,
                                AccountType.valueOf(accountType).isInternal() // TODO: ask user if this account is internal or not (maybe based on the account type or other factors)
                        ));
                });
    }

    @Bean
    public Command listAccounts() {
        return Command.builder()
                .name("account list")
                .description("List all accounts")
                .help("List all accounts. Usage: `account list`")
                .execute(ctx -> {
                    List<Account> accounts = accountService.getAllAccounts();

                    // Group by master type
                    Map<AccountType, List<Account>> accountsByType = accounts.stream()
                            .collect(Collectors.groupingBy(Account::getMasterType));

                    // Print each type
                    accountsByType.forEach((masterType, accountsInType) -> {
                        System.out.println("\u001B[1m\u001B[96m" + masterType + "\u001B[0m");
                        printAccountTree(accountsInType, null, 0);
                        System.out.println();
                    });
                });
    }

    @Bean
    public Command listAssets() {
        return Command.builder()
                .name("asset list")
                .description("List all assets")
                .help("List all assets. Usage: `asset ls`")
                .execute(ctx -> {
                    List<Asset> assets = assetService.getAllAssets();
                    assets.forEach(asset -> ctx.outputWriter().println(asset.getName() + " (ID: " + asset.getId() + ") - Symbol: " + asset.getSymbol() + " - Category: " + asset.getCategory()));
                });
    }


    // Ledger
    @Bean
    public Command addTransaction() {
        return Command.builder()
                .name("transaction add")
                .description("Add a new transaction")
                .help("Add a new transaction. Usage: `tx add --from-amount 100 --to-amount 100 --fee-amount 2.2 --date 2024-01-01 --description \"Grocery shopping\" --from-account-id 1 --to-account-id 2` \n Note: you can also use `--amount` option instead of `--from-amount` and `--to-amount` if the amounts are the same for both sides of the transaction.")
                .options(
                        CommandOption.with()
                                .shortName('a')
                                .longName("amount")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('f')
                                .longName("from-amount")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName("to-amount")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('d')
                                .longName("date")
                                .required(true)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('D')
                                .longName("description")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('F')
                                .longName("from-account-id")
                                .required(true)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('T')
                                .longName("to-account-id")
                                .required(true)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                            .shortName('e')
                            .longName("fee-amount")
                            .required(false)
                            .type(String.class)
                            .build())
                .execute(ctx -> {
                    String usage = "Usage: `tx add --from-amount 100 --to-amount 100 --fee-amount 2.2 --date 2024-01-01 --description \"Grocery shopping\" --from-account-id 1 --to-account-id 2` \n Note: you can also use `--amount` option instead of `--from-amount` and `--to-amount` if the amounts are the same for both sides of the transaction.";
                    String date = getOption(ctx, "date", "<date> option is missing. \n" + usage);
                    String fromAccountId = getOption(ctx, "from-account-id", "<from-account-id> option is missing. \n" + usage);
                    String toAccountId = getOption(ctx, "to-account-id", "<to-account-id> option is missing. \n" + usage);
                    String description = ctx.getOptionByLongName("description").value();

                    String amount = ctx.getOptionByLongName("amount").value();
                    String fromAmount;
                    String toAmount;
                    String feeAmount = ctx.getOptionByLongName("fee-amount").value() != null && !ctx.getOptionByLongName("fee-amount").value().isBlank() ? ctx.getOptionByLongName("fee-amount").value() : "0";

                    if (amount != null && !amount.isBlank()) {
                        fromAmount = amount;
                        toAmount = amount;
                    } else {
                        fromAmount = getOption(ctx, "from-amount", "<amount> or <from-amount> option is missing. \n" + usage);
                        toAmount = getOption(ctx, "to-amount", "<amount> or <to-amount> option is missing. \n" + usage);
                    }

                    ComponentFlow.ComponentFlowResult transactionTypeResult = componentFlowBuilder.clone().reset()
                            .withSingleItemSelector("transactionType")
                            .name("Transaction Type: ")
                            .selectItems(List.of(TransactionType.values()).stream()
                                    .map(transactionType -> SelectItem.of(transactionType.toString(), transactionType.toString()))
                                    .toList()).and().build().run();

                    TransactionCreateDto transactionCreateDto = new TransactionCreateDto(
                            description != null ? description : "",
                            TransactionType.valueOf(transactionTypeResult.getContext().get("transactionType", String.class)),
                            Long.valueOf(fromAccountId),
                            Long.valueOf(toAccountId),
                            new BigDecimal(fromAmount),
                            new BigDecimal(toAmount),
                            date,
                            SourceSystemCode.MANUAL_ENTRY,
                            new BigDecimal(feeAmount)
                    );

                    transactionService.createTransaction(transactionCreateDto);
                });
    }

    @Bean
    public Command listTransactions() {
        return Command.builder()
                .name("transaction list")
                .description("List all transactions")
                .help("List all transactions. Usage: `tx ls --transaction-type CARD_PURCHASE --from-account-id 1 --to-account-id 2 --start-date 2024-01-01 --end-date 2024-12-31` \n Note: all options are optional, you can filter transactions by from account, to account, start date, end date, and transaction type.")
                .options(
                        CommandOption.with()
                                .shortName('T')
                                .longName("transaction-type")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('f')
                                .longName("from-account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName("to-account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('s')
                                .longName("start-date")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('e')
                                .longName("end-date")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .execute(ctx -> {
                    String transactionType = ctx.getOptionByLongName("transaction-type") != null ? ctx.getOptionByLongName("transaction-type").value() : null;
                    String fromAccountId = ctx.getOptionByLongName("from-account-id") != null ? ctx.getOptionByLongName("from-account-id").value() : null;
                    String toAccountId = ctx.getOptionByLongName("to-account-id") != null ? ctx.getOptionByLongName("to-account-id").value() : null;
                    String startDate = ctx.getOptionByLongName("start-date") != null ? ctx.getOptionByLongName("start-date").value() : null;
                    String endDate = ctx.getOptionByLongName("end-date") != null ? ctx.getOptionByLongName("end-date").value() : null;

                    TransactionFilter transactionFilter = new TransactionFilter(
                            transactionType,
                            fromAccountId != null && !fromAccountId.isBlank() ? Long.valueOf(fromAccountId) : null,
                            toAccountId != null && !toAccountId.isBlank() ? Long.valueOf(toAccountId) : null,
                            startDate,
                            endDate
                    );

                    ctx.outputWriter().printf("%-30s | %-15s | %-30s | %-20s | %-20s | %-25s | %s%n", "Description", "Type", "From Account -> To Account", "From Amount", "To Amount", "Transaction Date", "Fee");
                    transactionService.getTransactions(transactionFilter)
                            .stream()
                            .forEach(
                                    transaction -> {
                                        String fromAccountName = transaction.getFromAccount().getName() + " (ID: " + transaction.getFromAccount().getId() + ")";
                                        String toAccountName = transaction.getToAccount().getName() + " (ID: " + transaction.getToAccount().getId() + ")";
                                        ctx.outputWriter().printf("%-30s | %-15s | %-30s | %-20s | %-20s | %-25s | %s%n",
                                                transaction.getDescription(),
                                                transaction.getType(),
                                                fromAccountName + " -> " + toAccountName,
                                                transaction.getFromAmount() + " " + assetSymbol(transaction.getFromAccount()),
                                                transaction.getToAmount() + " " + assetSymbol(transaction.getToAccount()),
                                                transaction.getTransactionDate(),
                                                transaction.getFeeAmount() + " " + assetSymbol(transaction.getFromAccount())
                                        );
                                    }
                            );
                });
    }


    // Ingestion
    @Bean
    public Command ingestTransactions() {
        return Command.builder()
                .name("ingest")
                .description("Ingest transactions from a file")
                .help("Ingest transactions from a file. Usage: `ingest --source HSBC_APP --file-path /path/to/transactions.csv`")
                .options(
                        CommandOption.with()
                                .shortName('f')
                                .longName("file-path")
                                .required(true)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('s')
                                .longName("source")
                                .required(true)
                                .type(String.class)
                                .build()
                )
                .execute(ctx -> {
                    String filePath = getOption(ctx, "file-path", "<file-path> option is missing. Usage: `ingest --source HSBC_APP --file-path /path/to/transactions.csv`");
                    String source = getOption(ctx, "source", "<source> option is missing. Usage : `ingest --source HSBC_APP --file-path /path/to/transactions.csv`");
                    try {
                        ingestionService.ingestTransactions(source, filePath, ctx);
                    } catch (IOException e) {
                        throw new IllegalStateException("Ingestion failed: " + e.getMessage(), e);
                    }
                });
    }

    private void printAccountTree(List<Account> accounts, Account parent, int level) {
        String indent = "  ".repeat(level);
        String treeConnector = level == 0 ? "" : "├─ ";

        accounts.stream()
                .filter(account -> {
                    // Root level: show accounts with no parent
                    if (parent == null) {
                        return account.getParent() == null;
                    }
                    // Child level: show accounts whose parent matches
                    return account.getParent() != null && account.getParent().getId().equals(parent.getId());
                })
                .forEach(account -> {
                    String assetLabel = account.getAsset() == null
                            ? "(folder)"
                            : account.getAsset().getName() + " (" + account.getAsset().getSymbol() + ")";
                    String line = indent + treeConnector +
                            "\u001B[1m\u001B[37m" + account.getName() + "\u001B[0m " +  // Bold white
                            "\u001B[2m\u001B[33m(ID: " + account.getId() + ")\u001B[0m " +  // Dim yellow
                            // REMOVED: "\u001B[92mType: " + account.getMasterType() + "\u001B[0m " +   // No longer needed
                            "\u001B[95mAsset: " + assetLabel + "\u001B[0m";  // Bright magenta

                    System.out.println(line);
                    printAccountTree(accounts, account, level + 1);
                });
    }

   private String validateAndGet(String value, String errorMessage) {
       if (value == null || value.isBlank()) {
           throw new IllegalArgumentException(errorMessage);
       }
       return value;
   }

   private String getOption(CommandContext ctx, String optionName, String errorMessage) {
       return validateAndGet(ctx.getOptionByLongName(optionName).value(), errorMessage);
   }

   private String getArgument(CommandContext ctx, int index, String errorMessage) {
       CommandArgument arg = ctx.getArgumentByIndex(index);
       return validateAndGet(arg != null ? arg.value() : null, errorMessage);
   }

   private static String assetSymbol(Account account) {
       return account.getAsset() == null ? "—" : account.getAsset().getSymbol();
   }
}
