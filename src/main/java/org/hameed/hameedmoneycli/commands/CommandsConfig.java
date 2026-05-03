package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AccountType;
import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.enums.SourceSystem;
import org.hameed.hameedmoneycli.enums.TransactionType;
import org.hameed.hameedmoneycli.model.dto.AccountCreateDto;
import org.hameed.hameedmoneycli.model.dto.AssetCreateDto;
import org.hameed.hameedmoneycli.model.dto.TransactionCreateDto;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.service.AccountService;
import org.hameed.hameedmoneycli.service.AssetService;
import org.hameed.hameedmoneycli.service.TransactionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandArgument;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CommandsConfig {

    private final ComponentFlow.Builder componentFlowBuilder;
    private final AssetService assetService;
    private final AccountService accountService;
    private final TransactionService transactionService;

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

                        // selecting the specific asset that this account accumulates
                    ComponentFlow.ComponentFlowResult assetResult = componentFlowBuilder.clone().reset()
                            .withSingleItemSelector("assetId")
                            .name("Select the asset that this account accumulates: ")
                            .selectItems(assetService.getAllAssets().stream()
                                    .map(asset -> SelectItem.of(asset.getName(), asset.getId().toString()))
                                    .toList())
                            .and().build().run();

                        String accountType = accountTypeResult.getContext().get("accountType", String.class);
                        String assetId = assetResult.getContext().get("assetId", String.class);
                        accountService.createAccount(new AccountCreateDto(
                                accountName,
                                AccountType.valueOf(accountType),
                                parentAccountId != null && !parentAccountId.isBlank() ? Long.valueOf(parentAccountId) : null,
                                Long.valueOf(assetId),
                                AccountType.valueOf(accountType).isInternal() // TODO: ask user if this account is internal or not (maybe based on the account type or other factors)
                        ));
                });
    }

    @Bean
    public Command listAccounts() {
        return Command.builder()
                .name("account list")
                .description("List all accounts")
                .help("List all accounts. Usage: `account ls --tree`")
                .options(
                        CommandOption.with()
                                .shortName('t')
                                .longName("tree")
                                .required(false)
                                .type(boolean.class)
                                .build())
                .execute(ctx -> {
                    List<Account> accounts = accountService.getAllAccounts();
                    boolean treeView = ctx.getOptionByLongName("tree").value() != null && Boolean.valueOf(ctx.getOptionByLongName("tree").value());
                    if (treeView) {
                        // print accounts in a tree view
                        printAccountTree(accounts, null, 0);
                    } else {
                        // print accounts in a flat list
                        accounts.forEach(account -> ctx.outputWriter().println(account.getName() + " (ID: " + account.getId() + ") - Type: " + account.getMasterType() + " - Asset: " + account.getAsset().getName() + " - Parent Account: " + (account.getParent() != null ? account.getParent().getName() : "None")));
                    }
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
                            SourceSystem.MANUAL_ENTRY,
                            new BigDecimal(feeAmount)
                    );

                    transactionService.createTransaction(transactionCreateDto);
                });
    }


    private void printAccountTree(List<Account> accounts, Account parent, int level) {
        accounts.stream()
                .filter(account -> (parent == null && account.getParent() == null) || (account.getParent() != null && account.getParent().getId().equals(parent.getId())))
                .forEach(account -> {
                    System.out.println("  ".repeat(level) + "- " + account.getName() + " (ID: " + account.getId() + ") - Type: " + account.getMasterType() + " - Asset: " + account.getAsset().getName());
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
}
