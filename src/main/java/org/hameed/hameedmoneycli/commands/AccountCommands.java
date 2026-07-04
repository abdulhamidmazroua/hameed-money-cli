package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AccountType;
import org.hameed.hameedmoneycli.model.dto.AccountCreateDto;
import org.hameed.hameedmoneycli.model.dto.AccountFilter;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.service.AccountService;
import org.hameed.hameedmoneycli.service.AssetService;
import org.hameed.hameedmoneycli.util.CommandsUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hameed.hameedmoneycli.util.CommandsUtil.*;

@Configuration
@RequiredArgsConstructor
public class AccountCommands {

    private final AccountService accountService;
    private final AssetService assetService;
    private final ComponentFlow.Builder componentFlowBuilder;

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

                    ComponentFlow.ComponentFlowResult accountTypeResult = componentFlowBuilder.clone().reset()
                            .withSingleItemSelector("accountType")
                            .name("Account Type: ")
                            .selectItems(List.of(AccountType.values()).stream()
                                    .map(accountType -> SelectItem.of(accountType.toString(), accountType.toString()))
                                    .toList())
                            .and().build().run();

                    List<SelectItem> assetChoices = new ArrayList<>();
                    assetChoices.add(SelectItem.of("(Folder \u2014 organizational only, no asset / not for postings)", ""));
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
                            AccountType.fromString(accountType).isInternal()
                    ));
                });
    }

    @Bean
    public Command initAccount() {
        return Command.builder()
                .name("account init")
                .description("Create an account and post its opening balance in one shot")
                .help("Create an account and post its opening balance. Usage: `account init --name \"Wallet\" --asset EGP --balance 1000 --parent-account-id 5` \nNote: the account name is auto-prefixed with the asset symbol (e.g., \"EGP:Wallet\"). Also accepts --category (default cash). If the asset does not exist, it is registered automatically.")
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

                    Long parentAccountId = parentId != null && !parentId.isBlank() ? Long.valueOf(parentId) : null;

                    Account account = accountService.createAccountWithOpeningBalance(accountName, parentAccountId, assetSymbol, categoryArg, balanceStr);
                    ctx.outputWriter().println("Account '" + account.getName() + "' created with opening balance " + balanceStr + " " + assetSymbol + ".");
                });
    }

    @Bean
    public Command listAccounts() {
        return Command.builder()
                .name("account list")
                .description("List all accounts in a tree view")
                .help("List all accounts grouped by type (ASSET, LIABILITY, INCOME, EXPENSE, SYSTEM) in a hierarchical tree. Usage: `account list`")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    List<Account> accounts = accountService.getAllAccounts();

                    Map<AccountType, List<Account>> accountsByType = accounts.stream()
                            .collect(Collectors.groupingBy(Account::getMasterType));

                    accountsByType.forEach((masterType, accountsInType) -> {
                        System.out.println("\u001B[1m\u001B[96m" + masterType + "\u001B[0m");
                        CommandsUtil.printAccountTree(accountsInType, null, 0);
                        System.out.println();
                    });
                });
    }

    @Bean
    public Command findAccounts() {
        return Command.builder()
                .name("account find")
                .description("Search accounts by keyword, type, or asset symbol")
                .help("Search accounts by name (keyword), master type (ASSET, EXPENSE, etc.), or asset symbol. Usage: `account find hsbc` or `account find --type expense` or `account find --asset EGP` or `account find` (lists all)")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('t')
                                .longName("type")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('a')
                                .longName("asset")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .execute(ctx -> {
                    String keyword = argOrOption(ctx, 0, 'k', "keyword");
                    String masterType = getOptionOrDefault(ctx, 't', "type", null);
                    String assetSymbol = getOptionOrDefault(ctx, 'a', "asset", null);

                    List<Account> results = accountService.findAccounts(new AccountFilter(keyword, masterType, assetSymbol));

                    if (results.isEmpty()) {
                        ctx.outputWriter().println("No accounts found matching the given filters.");
                        return;
                    }

                    ctx.outputWriter().printf("%-4s | %-40s | %-10s | %-10s | %s%n", "ID", "Name", "Type", "Asset", "Internal");
                    ctx.outputWriter().printf("%-4s-|-%40s-|-%10s-|-%10s-|-%s%n",
                            "----", "----------------------------------------", "----------", "----------", "--------");
                    for (Account account : results) {
                        String assetName = account.getAsset() == null ? "\u2014" : account.getAsset().getSymbol();
                        ctx.outputWriter().printf("%-4d | %-40s | %-10s | %-10s | %s%n",
                                account.getId(),
                                truncate(account.getName(), 40),
                                account.getMasterType(),
                                assetName,
                                account.getIsInternal());
                    }
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

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
