package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.*;
import org.hameed.hameedmoneycli.model.dto.*;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.service.*;
import org.hameed.hameedmoneycli.util.DateUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.*;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.availability.AvailabilityProvider;
import org.springframework.shell.core.command.exit.ExitStatusExceptionMapper;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private final IngestionRuleService ingestionRuleService;
    private final MarketQuoteService marketQuoteService;
    private final ReportService reportService;
    private final AuditService auditService;
    private final SystemAdjustmentService systemAdjustmentService;
    private final BackupService backupService;

    // Assets and Accounts

    @Bean
    public Command getCategories() {
        return Command.builder()
                .name("cat-list")
                .description("List all asset categories")
                .help("List all asset categories. Usage: `cat-list`")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    for (AssetCategory category : AssetCategory.values()) {
                        ctx.outputWriter().println(category.getCategory());
                    }
                });
    }

    @Bean
    public Command fetchAssetData() {
        return Command.builder()
                .name("asset fetch")
                .description("Fetch asset data")
                .help("Fetch asset data. Usage: `asset fetch stock EGX` or `asset fetch --category stock --exchange EGX`")
                .options(CommandOption.with()
                        .shortName('c')
                        .longName("category")
                        .type(String.class)
                        .required(false)
                        .build(),
                        CommandOption.with()
                                .shortName('e')
                                .longName("exchange")
                                .type(String.class)
                                .required(false)
                                .build())
                .execute(ctx -> {
                    String category = argOrOption(ctx, 0, 'c', "category");
                    if (category == null) throw new IllegalArgumentException(required("category"));
                    AssetCategory assetCategory = AssetCategory.fromString(category);

                    if (assetCategory != AssetCategory.STOCK && assetCategory != AssetCategory.ETF && assetCategory != AssetCategory.MUTUAL_FUND) {
                        throw new IllegalArgumentException("Unsupported category: " + category + ". Use stock, etf, or fund.");
                    }
                    String exchange = argOrOption(ctx, 1, 'e', "exchange");
                    if (exchange == null) throw new IllegalArgumentException(required("exchange"));
                    assetService.syncAssetData(StockExchange.fromString(exchange), assetCategory);
                });
    }

    @Bean
    public Command registerAsset() {
        return Command.builder()
                .name("asset register")
                .description("Register a new asset")
                .help("Register a new asset. Usage: `asset register \"Commercial International Bank\" COMI.CA` or `asset register --name \"Bank\" --symbol BNK`")
                .options(CommandOption.with()
                                .shortName('n')
                                .longName("name")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('s')
                                .longName("symbol")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('c')
                                .longName("category")
                                .required(false)
                                .type(String.class)
                                .build())
                .exitStatusExceptionMapper(exceptionMapper())
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    String assetName = argOrOption(ctx, 0, 'n', "name");
                    if (assetName == null) throw new IllegalArgumentException(required("name"));
                    String symbol = argOrOption(ctx, 1, 's', "symbol");
                    if (symbol == null) throw new IllegalArgumentException(required("symbol"));
                    String categoryArg = getOptionOrDefault(ctx, 'c', "category", null);

                    String assetCategory;
                    if (categoryArg != null) {
                        assetCategory = categoryArg;
                    } else {
                        ComponentFlow.ComponentFlowResult assetCategoryResult = componentFlowBuilder.clone().reset()
                                .withSingleItemSelector("assetCategory")
                                .name("Asset Category: ")
                                .selectItems(List.of(AssetCategory.values()).stream()
                                        .map(cat -> SelectItem.of(cat.getCategory(), cat.getCategory()))
                                        .toList()).and().build().run();
                        assetCategory = assetCategoryResult.getContext().get("assetCategory", String.class);
                    }
                    assetService.createAsset(new AssetCreateDto(assetName, symbol, AssetCategory.fromString(assetCategory), AssetCategory.fromString(assetCategory).isTradable()));
                });
    }

    @Bean
    public Command createAccount() {
        return Command.builder()
                .name("account create")
                .description("Create a new account")
                .help("Create a new account. Usage: `account create --name \"Cash Account\" --parent-account-id 3` or `--parent-account-name \"Cash\"`")
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
                                .build(),
                        CommandOption.with()
                                .shortName('P')
                                .longName("parent-account-name")
                                .required(false)
                                .type(String.class)
                                .build())
                .exitStatusExceptionMapper(exceptionMapper())
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    String usage = "Usage: `account create --name \"Cash Account\" --parent-account-id 3`";
                    String accountName = getOptionOrError(ctx, 'n', "name", "<name> option is missing. \n" + usage);
                    String parentAccountId = getOptionOrDefault(ctx, 'p', "parent-account-id", null);
                    String parentAccountName = getOptionOrDefault(ctx, 'P', "parent-account-name", null);

                    if (parentAccountId == null && parentAccountName != null) {
                        parentAccountId = accountService.getAccountByName(parentAccountName).getId().toString();
                    }

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
                            AccountType.fromString(accountType),
                            parentAccountId != null && !parentAccountId.isBlank() ? Long.valueOf(parentAccountId) : null,
                            assetIdLong,
                            AccountType.fromString(accountType).isInternal() // TODO: ask user if this account is internal or not (maybe based on the account type or other factors)
                    ));
                });
    }

    @Bean
    public Command initAccount() {
        return Command.builder()
                .name("account init")
                .description("Create an account and post its opening balance in one shot")
                .help("Create an account and post its opening balance. Usage: `account init --name \"XYZ Fund\" --asset XYZ --balance 1000 --parent-account-id 5` \nAlso accepts --category (default cash). If the asset does not exist, it is registered automatically.")
                .options(
                        CommandOption.with()
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
                                .build(),
                        CommandOption.with()
                                .shortName('a')
                                .longName("asset")
                                .required(true)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('c')
                                .longName("category")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('b')
                                .longName("balance")
                                .required(true)
                                .type(String.class)
                                .build())
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String usage = "Usage: `account init --name \"XYZ Fund\" --asset XYZ --balance 1000`";
                    String accountName = getOptionOrError(ctx, 'n', "name", "<name> option is missing. \n" + usage);
                    String parentId = getOptionOrDefault(ctx, 'p', "parent-account-id", null);
                    String assetSymbol = getOptionOrError(ctx, 'a', "asset", "<asset> option is missing. \n" + usage);
                    String categoryArg = getOptionOrDefault(ctx, 'c', "category", "cash");
                    String balanceStr = getOptionOrError(ctx, 'b', "balance", "<balance> option is missing. \n" + usage);

                    AssetCategory category = AssetCategory.fromString(categoryArg);
                    Asset asset;
                    try {
                        asset = assetService.getAssetBySymbolAndCategory(assetSymbol, category);
                    } catch (IllegalArgumentException e) {
                        assetService.createAsset(new AssetCreateDto(assetSymbol, assetSymbol, category, category.isTradable()));
                        asset = assetService.getAssetBySymbolAndCategory(assetSymbol, category);
                    }

                    Long parentAccountId = parentId != null && !parentId.isBlank() ? Long.valueOf(parentId) : null;

                    accountService.createAccount(new AccountCreateDto(
                            accountName,
                            AccountType.ASSET,
                            parentAccountId,
                            asset.getId(),
                            true
                    ));

                    systemAdjustmentService.initAccount(accountName, new BigDecimal(balanceStr));
                    ctx.outputWriter().println("Account '" + accountName + "' created with opening balance " + balanceStr + " " + assetSymbol + ".");
                });
    }

    @Bean
    public Command listAccounts() {
        return Command.builder()
                .name("account list")
                .description("List all accounts")
                .help("List all accounts. Usage: `account list`")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
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
    public Command deleteAccount() {
        return Command.builder()
                .name("account delete")
                .description("Delete an account")
                .help("Delete an account. Usage: `account delete 5` or `account delete --account-id 5`")
                .options(CommandOption.with()
                        .shortName('a')
                        .longName("account-id")
                        .required(false)
                        .type(String.class)
                        .build())
                .exitStatusExceptionMapper(exceptionMapper())
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    String accountIdStr = argOrOption(ctx, 0, 'a', "account-id");

                    if (accountIdStr == null) {
                        List<SelectItem> accountChoices = accountService.getAllAccounts().stream()
                                .map(acct -> SelectItem.of(
                                        acct.getName() + " (ID: " + acct.getId() + ")",
                                        acct.getId().toString()))
                                .toList();
                        ComponentFlow.ComponentFlowResult result = componentFlowBuilder.clone().reset()
                                .withSingleItemSelector("accountId")
                                .name("Select account to delete:")
                                .selectItems(accountChoices)
                                .and().build().run();
                        accountIdStr = result.getContext().get("accountId", String.class);
                    }

                    Long accountId = Long.valueOf(accountIdStr);
                    accountService.deleteAccount(accountId);
                    ctx.outputWriter().println("Deleted account ID " + accountId);
                });
    }

    @Bean
    public Command listAssets() {
        return Command.builder()
                .name("asset list")
                .description("List all assets")
                .help("List all assets. Usage: `asset list`")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
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
                .help("Add a new transaction. Usage: `transaction add --from-amount 100 --to-amount 100 --fee-amount 2.2 --date 2024-01-01 --description \"Grocery shopping\" --from-account-id 1 --to-account-id 2` \n Note: you can also use `--amount` option instead of `--from-amount` and `--to-amount` if the amounts are the same for both sides of the transaction. \n Instead of `--from-account-id`/`--to-account-id` you can use `--from-account-name`/`--to-account-name`.")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
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
                                .required(false)
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
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('T')
                                .longName("to-account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('N')
                                .longName("from-account-name")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('M')
                                .longName("to-account-name")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('e')
                                .longName("fee-amount")
                                .required(false)
                                .type(String.class)
                                .build())
                .execute(ctx -> {
                    String usage = "Usage: `transaction add --from-amount 100 --to-amount 100 --fee-amount 2.2 --date 2024-01-01 --description \"Grocery shopping\" --from-account-id 1 --to-account-id 2` \n Note: you can also use `--amount` option instead of `--from-amount` and `--to-amount` if the amounts are the same for both sides of the transaction.";
                    String date = getOptionOrDefault(ctx, 'd', "date", LocalDate.now().format(DateTimeFormatter.ofPattern(DateUtil.DEFAULT_DATE_FORMAT)));
                    String fromAccountId = getOptionOrDefault(ctx, 'F', "from-account-id", null);
                    String toAccountId = getOptionOrDefault(ctx, 'T', "to-account-id", null);
                    String fromAccountName = getOptionOrDefault(ctx, 'N', "from-account-name", null);
                    String toAccountName = getOptionOrDefault(ctx, 'M', "to-account-name", null);

                    if (fromAccountId == null && fromAccountName != null) {
                        fromAccountId = accountService.getAccountByName(fromAccountName).getId().toString();
                    }
                    if (toAccountId == null && toAccountName != null) {
                        toAccountId = accountService.getAccountByName(toAccountName).getId().toString();
                    }
                    if (fromAccountId == null) {
                        throw new IllegalArgumentException("Either --from-account-id or --from-account-name is required.");
                    }
                    if (toAccountId == null) {
                        throw new IllegalArgumentException("Either --to-account-id or --to-account-name is required.");
                    }
                    String description = getOptionOrDefault(ctx, 'D', "description", null);
                    String feeAmount = getOptionOrDefault(ctx, 'e', "fee-amount", "0");

                    String amount = getOptionOrDefault(ctx, 'a', "amount", null);
                    String fromAmount;
                    String toAmount;

                    if (amount != null && !amount.isBlank()) {
                        fromAmount = amount;
                        toAmount = amount;
                    } else {
                        fromAmount = getOptionOrError(ctx, 'f', "from-amount", "<amount> or <from-amount> option is missing. \n" + usage);
                        toAmount = getOptionOrError(ctx, 't', "to-amount", "<amount> or <to-amount> option is missing. \n" + usage);
                    }

                    ComponentFlow.ComponentFlowResult transactionTypeResult = componentFlowBuilder.clone().reset()
                            .withSingleItemSelector("transactionType")
                            .name("Transaction Type: ")
                            .selectItems(List.of(TransactionType.values()).stream()
                                    .map(transactionType -> SelectItem.of(transactionType.toString(), transactionType.toString()))
                                    .toList()).and().build().run();

                    TransactionCreateDto transactionCreateDto = new TransactionCreateDto(
                            description != null ? description : "",
                            TransactionType.fromString(transactionTypeResult.getContext().get("transactionType", String.class)),
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
                .help("List all transactions. Usage: `transaction list --transaction-type CARD_PURCHASE --from-account-id 1 --to-account-id 2 --start-date 2024-01-01 --end-date 2024-12-31` \n Note: all options are optional, you can filter transactions by from account, to account, start date, end date, and transaction type.")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())

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
                            String transactionType = getOptionOrDefault(ctx, 'T', "transaction-type", null);
                            String fromAccountId = getOptionOrDefault(ctx, 'f', "from-account-id", null);
                            String toAccountId = getOptionOrDefault(ctx, 't', "to-account-id", null);
                            String startDate = getOptionOrDefault(ctx, 's', "start-date", null);
                            String endDate = getOptionOrDefault(ctx, 'e', "end-date", null);

                            TransactionFilter transactionFilter = new TransactionFilter(
                                    transactionType,
                                    fromAccountId != null && !fromAccountId.isBlank() ? Long.valueOf(fromAccountId) : null,
                                    toAccountId != null && !toAccountId.isBlank() ? Long.valueOf(toAccountId) : null,
                                    startDate,
                                    endDate
                            );

                            ctx.outputWriter().printf("%-12s | %-50s | %-18s | %-18s | %-20s | %-12s%n", "Type", "From Account -> To Account", "From Amount", "To Amount", "Date", "Fee");
                            transactionService.getTransactions(transactionFilter)
                                    .stream()
                                    .forEach(
                                            transaction -> {
                                                String fromAccountName = transaction.getFromAccount().getName() + " (ID: " + transaction.getFromAccount().getId() + ")";
                                                String toAccountName = transaction.getToAccount().getName() + " (ID: " + transaction.getToAccount().getId() + ")";
                                                String accountPair = fromAccountName + " -> " + toAccountName;
                                                ctx.outputWriter().printf("%-12s | %-50s | %-18s | %-18s | %-20s | %-12s%n",
                                                        transaction.getType(),
                                                        accountPair.length() > 50 ? accountPair.substring(0, 47) + "..." : accountPair,
                                                        transaction.getFromAmount() + " " + assetSymbol(transaction.getFromAccount()),
                                                        transaction.getToAmount() + " " + assetSymbol(transaction.getToAccount()),
                                                        transaction.getTransactionDate(),
                                                        transaction.getFeeAmount() + " " + assetSymbol(transaction.getFromAccount())
                                                );
                                            }
                                    );
                        }
                );
    }

    @Bean
    public Command generateTransactionReport() {
        return Command.builder()
                .name("transaction report")
                .description("Generate a transaction report")
                .help("Generate a transaction report. Usage: `transaction report --transaction-type CARD_PURCHASE --from-account-id 1 --to-account-id 2 --start-date 2024-01-01 --end-date 2024-12-31` \n Note: all options are optional, you can filter transactions by from account, to account, start date, end date, and transaction type.")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())

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
                    String transactionType = getOptionOrDefault(ctx, 'T', "transaction-type", null);
                    String fromAccountId = getOptionOrDefault(ctx, 'f', "from-account-id", null);
                    String toAccountId = getOptionOrDefault(ctx, 't', "to-account-id", null);
                    String startDate = getOptionOrDefault(ctx, 's', "start-date", null);
                    String endDate = getOptionOrDefault(ctx, 'e', "end-date", null);

                    TransactionFilter transactionFilter = new TransactionFilter(
                            transactionType,
                            fromAccountId != null && !fromAccountId.isBlank() ? Long.valueOf(fromAccountId) : null,
                            toAccountId != null && !toAccountId.isBlank() ? Long.valueOf(toAccountId) : null,
                            startDate,
                            endDate);
                    try {
                        transactionService.generateTransactionReport(transactionFilter);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }


    // Ingestion
    @Bean
    public Command ingestTransactions() {
        return Command.builder()
                .name("ingest")
                .description("Ingest transactions from a file")
                .help("Ingest transactions from a file. Usage: `ingest HSBC_APP /path/to/transactions.csv` or `ingest --source HSBC_APP --file-path /path/to/transactions.csv`")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())

                .options(
                        CommandOption.with()
                                .shortName('f')
                                .longName("file-path")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('s')
                                .longName("source")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .execute(ctx -> {
                    String source = argOrOption(ctx, 0, 's', "source");
                    if (source == null) throw new IllegalArgumentException(required("source"));
                    String filePath = argOrOption(ctx, 1, 'f', "file-path");
                    if (filePath == null) throw new IllegalArgumentException(required("file-path"));
                    try {
                        ingestionService.ingestTransactions(source, filePath, ctx);
                    } catch (IOException e) {
                        throw new IllegalStateException("Ingestion failed: " + e.getMessage(), e);
                    }
                });
    }

    @Bean
    public Command addRule() {
        return Command.builder()
                .name("rule add")
                .description("Add a new ingestion rule")
                .help("Add a new ingestion rule. Usage: `rule add \"regex\" 5` or `rule add --pattern \"regex\" --target 5`")
                .options(CommandOption.with()
                                .shortName('p')
                                .longName("pattern")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName("target")
                                .required(false)
                                .type(String.class)
                                .build())
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String match = argOrOption(ctx, 0, 'p', "pattern");
                    if (match == null) throw new IllegalArgumentException(required("pattern"));
                    String target = argOrOption(ctx, 1, 't', "target");
                    if (target == null) throw new IllegalArgumentException(required("target"));

                    ingestionRuleService.addRule(new RuleCreateDto(
                            match,
                            Long.valueOf(target)
                    ));
                });
    }

    @Bean
    public Command setQuote() {
        return Command.builder()
                .name("quote set")
                .description("Set the latest price for an asset")
                .help("Set the latest price for an asset. Usage: `quote set USD EGP --price 48.5` or `quote set --base USD --quote EGP --price 48.5`")
                .options(
                        CommandOption.with()
                                .shortName('b')
                                .longName("base")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('q')
                                .longName("quote")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('p')
                                .longName("price")
                                .required(true)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('d')
                                .longName("date")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String baseSymbol = argOrOption(ctx, 0, 'b', "base");
                    if (baseSymbol == null) throw new IllegalArgumentException(required("base"));
                    String quoteSymbol = argOrOption(ctx, 1, 'q', "quote");
                    if (quoteSymbol == null) throw new IllegalArgumentException(required("quote"));
                    String price = getOptionOrError(ctx, 'p', "price", required("price"));
                    String date = getOptionOrDefault(ctx, 'd', "date", null);
                    marketQuoteService.setMarketQuote(new MarketQuoteDto(
                            baseSymbol,
                            quoteSymbol,
                            new BigDecimal(price),
                            date
                    ));
                });
    }

    @Bean
    public Command getQuote() {
        return Command.builder()
                .name("quote get")
                .description("Get the latest price for an asset")
                .help("Get the latest price for an asset. Usage: `quote get AAPL USD` or `quote get --base AAPL --quote USD`")
                .options(
                        CommandOption.with()
                                .shortName('b')
                                .longName("base")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('q')
                                .longName("quote")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String baseSymbol = argOrOption(ctx, 0, 'b', "base");
                    if (baseSymbol == null) throw new IllegalArgumentException(required("base"));
                    String quoteSymbol = argOrOption(ctx, 1, 'q', "quote");
                    if (quoteSymbol == null) throw new IllegalArgumentException(required("quote"));

                    List<MarketQuoteDto> marketQuotes = marketQuoteService.getMarketQuote(baseSymbol, quoteSymbol);
                    marketQuotes.forEach(quote -> ctx.outputWriter().println("Price of " + quote.baseSymbol() + " in " + quote.quoteSymbol() + " is " + quote.price() + " (as of " + quote.marketQuoteDate() + ")"));
                });
    }

    @Bean
    public Command fetchQuote() {
        return Command.builder()
                .name("quote fetch")
                .description("Fetch the latest quote from Yahoo Finance and save it")
                .help("Fetch the latest quote from Yahoo Finance and save it. Usage: `quote fetch AAPL USD` or `quote fetch --base AAPL --quote USD`")
                .options(
                        CommandOption.with()
                                .shortName('b')
                                .longName("base")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('q')
                                .longName("quote")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String baseSymbol = argOrOption(ctx, 0, 'b', "base");
                    if (baseSymbol == null) throw new IllegalArgumentException(required("base"));
                    String quoteSymbol = argOrOption(ctx, 1, 'q', "quote");
                    if (quoteSymbol == null) throw new IllegalArgumentException(required("quote"));

                    marketQuoteService.fetchAndSaveQuote(baseSymbol, quoteSymbol);
                    ctx.outputWriter().println("Saved quote: " + baseSymbol + " -> " + quoteSymbol);
                });
    }

    @Bean
    public Command listQuotes() {
        return Command.builder()
                .name("quote list")
                .description("List all stored market quotes")
                .help("List the latest quote for each asset pair. Usage: `quote list`")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    List<MarketQuoteDto> quotes = marketQuoteService.listMarketQuotes();
                    if (quotes.isEmpty()) {
                        ctx.outputWriter().println("No market quotes stored.");
                        return;
                    }
                    String format = "%-12s %-12s %-12s %s";
                    ctx.outputWriter().println(String.format(format, "Base", "Quote", "Price", "Date"));
                    ctx.outputWriter().println(String.format(format, "----", "-----", "-----", "----"));
                    for (MarketQuoteDto q : quotes) {
                        ctx.outputWriter().println(String.format(format, q.baseSymbol(), q.quoteSymbol(), q.price(), q.marketQuoteDate()));
                    }
                });
    }

    @Bean
    public Command reportNetworth() {
        return Command.builder()
                .name("report nw")
                .description("Generate balance sheet net worth report valued in a specific currency")
                .help("Generate a financial report. Usage: `report nw --currency EGP` or just `report nw` (defaults to EGP).")
                .options(CommandOption.with()
                        .shortName('c')
                        .longName("currency")
                        .required(false)
                        .type(String.class)
                        .defaultValue("EGP")
                        .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String currency = argOrOption(ctx, 0, 'c', "currency", "EGP");
                    reportService.generateNetworthReport(currency).terminalPrint(ctx.outputWriter());
                });
    }

    @Bean
    public Command reportDataIntegrity() {
        return Command.builder()
                .name("report data-integrity")
                .description("Generate a data integrity audit report")
                .help("Generate a data integrity audit report. Usage: `report data-integrity`")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    reportService.generateDataIntegrityReport().terminalPrint(ctx.outputWriter());
                });
    }

    @Bean
    public Command auditAccount() {
        return Command.builder()
                .name("audit account")
                .description("Audit an account — verify computed balance")
                .help("Audit an account. Usage: `audit account 5` or `audit account --id 5` or `--name \"Foo\"`")
                .options(
                        CommandOption.with()
                                .shortName('i')
                                .longName("id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('n')
                                .longName("name")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    // try positional arg first (as ID), then --id, then --name
                    var args = ctx.parsedInput().arguments();
                    Long accountId = null;
                    if (!args.isEmpty()) {
                        String raw = args.getFirst().value();
                        // if it looks like a number, treat as ID
                        if (raw.matches("\\d+")) {
                            accountId = Long.valueOf(raw);
                        } else {
                            // treat as name
                            accountId = accountService.getAccountByName(raw).getId();
                        }
                    } else {
                        String idStr = getOptionOrDefault(ctx, 'i', "id", null);
                        String name = getOptionOrDefault(ctx, 'n', "name", null);
                        if (idStr != null) {
                            accountId = Long.valueOf(idStr);
                        } else if (name != null) {
                            accountId = accountService.getAccountByName(name).getId();
                        }
                    }

                    if (accountId == null) {
                        List<SelectItem> choices = accountService.getAllAccounts().stream()
                                .map(a -> SelectItem.of(
                                        a.getName() + " (ID: " + a.getId() + ")",
                                        a.getId().toString()))
                                .toList();
                        ComponentFlow.ComponentFlowResult result = componentFlowBuilder.clone().reset()
                                .withSingleItemSelector("accountId")
                                .name("Select account to audit:")
                                .selectItems(choices)
                                .and().build().run();
                        accountId = Long.valueOf(result.getContext().get("accountId", String.class));
                    }

                    ctx.outputWriter().println(auditService.auditAccount(accountId));
                });
    }

    @Bean
    public Command auditTrail() {
        return Command.builder()
                .name("audit trail")
                .description("Full ledger audit — verify data integrity across all accounts")
                .help("Run a full ledger audit. Usage: `audit trail`")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    ctx.outputWriter().println(auditService.auditTrail());
                });
    }

    @Bean
    public Command hmcInit() {
        return Command.builder()
                .name("hmc init")
                .description("Initialize an account with an opening balance (system adjustment)")
                .help("Initialize an account with an opening balance. Usage: `hmc init \"HSBC Current\" --balance 50000` or `hmc init --account \"HSBC Current\" --balance 50000`")
                .options(
                        CommandOption.with()
                                .shortName('a')
                                .longName("account")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('i')
                                .longName("account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('b')
                                .longName("balance")
                                .required(true)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String name = argOrOption(ctx, 0, 'a', "account");
                    String idStr = getOptionOrDefault(ctx, 'i', "account-id", null);
                    String balance = getOptionOrError(ctx, 'b', "balance", required("balance"));

                    if (idStr != null) {
                        systemAdjustmentService.initAccount(Long.valueOf(idStr), new BigDecimal(balance));
                    } else if (name != null) {
                        systemAdjustmentService.initAccount(name, new BigDecimal(balance));
                    } else {
                        throw new IllegalArgumentException("Either --account <name>, --account-id <id>, or a positional account name is required.");
                    }

                    ctx.outputWriter().println("Opening balance of " + balance + " posted.");
                });
    }

    @Bean
    public Command hmcReconcile() {
        return Command.builder()
                .name("hmc reconcile")
                .description("Reconcile an account's computed balance with the actual balance")
                .help("Reconcile an account. Usage: `hmc reconcile \"HSBC Current\" --actual 49990` or `hmc reconcile --account \"HSBC Current\" --actual 49990`")
                .options(
                        CommandOption.with()
                                .shortName('a')
                                .longName("account")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('i')
                                .longName("account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('c')
                                .longName("actual")
                                .required(true)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String name = argOrOption(ctx, 0, 'a', "account");
                    String idStr = getOptionOrDefault(ctx, 'i', "account-id", null);
                    String actual = getOptionOrError(ctx, 'c', "actual", required("actual"));

                    if (idStr != null) {
                        systemAdjustmentService.reconcileAccount(Long.valueOf(idStr), new BigDecimal(actual));
                    } else if (name != null) {
                        systemAdjustmentService.reconcileAccount(name, new BigDecimal(actual));
                    } else {
                        throw new IllegalArgumentException("Either --account <name>, --account-id <id>, or a positional account name is required.");
                    }

                    ctx.outputWriter().println("Reconciled to actual balance " + actual);
                });
    }

    @Bean
    public Command dbBackup() {
        return Command.builder()
                .name("hmc db backup")
                .description("Backup the database with pg_dump")
                .help("Backup the database. Usage: `hmc db backup` or `hmc db backup --output ~/hmc/backups`")
                .options(
                        CommandOption.with()
                                .longName("output")
                                .shortName('o')
                                .required(false)
                                .type(String.class)
                                .defaultValue("~/hmc/backups")
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    try {
                        String outputDir = getOptionOrDefault(ctx, 'o', "output", "~/hmc/backups");
                        outputDir = outputDir.replaceFirst("^~", System.getProperty("user.home"));

                        Path backupFile = backupService.backup(outputDir);
                        ctx.outputWriter().println("Backup saved: " + backupFile.toAbsolutePath());
                    } catch (Exception e) {
                        throw new RuntimeException("Backup failed: " + e.getMessage(), e);
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

    private CommandOption getOption(CommandContext ctx, char shortName, String longName) {
        CommandOption option = ctx.getOptionByLongName(longName);
        return option != null ? option : ctx.getOptionByShortName(shortName);
    }

    private String getOptionOrDefault(CommandContext ctx, char shortName, String longName, String defaultVal) {
        CommandOption option = getOption(ctx, shortName, longName);
        return isOptionValid(option) ? option.value() : defaultVal;
    }

    private String getOptionOrError(CommandContext ctx, char shortName, String longName, String errorMessage) {
        CommandOption option = getOption(ctx, shortName, longName);
        if (isOptionValid(option)) {
            return option.value();
        }
        throw new IllegalArgumentException(errorMessage);
    }

    private boolean isOptionValid(CommandOption option) {
        return option != null && option.value() != null && !option.value().isBlank();
    }

    private String argOrOption(CommandContext ctx, int argIndex, char shortName, String longName) {
        var args = ctx.parsedInput().arguments();
        if (argIndex < args.size()) {
            return args.get(argIndex).value();
        }
        return getOptionOrDefault(ctx, shortName, longName, null);
    }

    private String argOrOption(CommandContext ctx, int argIndex, char shortName, String longName, String defaultVal) {
        var args = ctx.parsedInput().arguments();
        if (argIndex < args.size()) {
            return args.get(argIndex).value();
        }
        return getOptionOrDefault(ctx, shortName, longName, defaultVal);
    }

    private String required(String label) {
        return "<" + label + "> is required.";
    }

    private static String assetSymbol(Account account) {
        return account.getAsset() == null ? "—" : account.getAsset().getSymbol();
    }


    private ExitStatusExceptionMapper exceptionMapper() {
        return exception -> {
            if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
                return new ExitStatus(1, exception.getMessage());
            }
            return new ExitStatus(2, "An unexpected error occurred: " + exception.getMessage());
        };
    }

    private AvailabilityProvider availabilityProvider() {
        return Availability::available;
    }
}
