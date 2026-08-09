package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AccountType;
import org.hameed.hameedmoneycli.model.dto.AccountCreateDto;
import org.hameed.hameedmoneycli.model.dto.AccountFilter;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.repository.AccountRepository;
import org.hameed.hameedmoneycli.repository.TransactionRepository;
import org.hameed.hameedmoneycli.service.AccountService;
import org.hameed.hameedmoneycli.service.AssetService;
import org.hameed.hameedmoneycli.util.CommandsUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hameed.hameedmoneycli.util.CommandsUtil.*;
import static org.hameed.hameedmoneycli.constants.CommandConstants.*;

@Configuration
@RequiredArgsConstructor
public class AccountCommands {

    private final AccountService accountService;
    private final AssetService assetService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ComponentFlow.Builder componentFlowBuilder;

    @Bean
    public Command createAccount() {
        return Command.builder()
                .name("account create")
                .description(ACCOUNT_CREATE_COMMAND_DESCRIPTION)
                .help(ACCOUNT_CREATE_COMMAND_HELP)
                .options(CommandOption.with()
                                .shortName('n')
                                .longName(NAME_ARG)
                                .required(true)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('p')
                                .longName(PARENT_ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('P')
                                .longName(PARENT_ACCOUNT_NAME_ARG)
                                .required(false)
                                .type(String.class)
                                .build())
                .exitStatusExceptionMapper(exceptionMapper())
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    String accountName = argOrOption(ctx, 0, 'n', NAME_ARG);
                    if (accountName == null) throw new IllegalArgumentException(ACCOUNT_CREATE_NAME_ARG_ERROR);
                    String parentAccountId = getOptionOrDefault(ctx, 'p', PARENT_ACCOUNT_ID_ARG, null);
                    String parentAccountName = getOptionOrDefault(ctx, 'P', PARENT_ACCOUNT_NAME_ARG, null);

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
                .description(INIT_ACCOUNT_COMMAND_DESCRIPTION)
                .help(INIT_ACCOUNT_COMMAND_HELP)
                .options(
                        CommandOption.with()
                                .shortName('n')
                                .longName(NAME_ARG)
                                .required(true)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('p')
                                .longName(PARENT_ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('a')
                                .longName(ASSET_ARG)
                                .required(true)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('b')
                                .longName(BALANCE_ARG)
                                .required(true)
                                .type(String.class)
                                .build())
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String accountName = argOrOption(ctx, 0, 'n', NAME_ARG);
                    if (accountName == null) throw new IllegalArgumentException(INIT_ACCOUNT_NAME_ARG_ERROR);
                    String parentId = getOptionOrDefault(ctx, 'p', PARENT_ACCOUNT_ID_ARG, null);
                    String assetSymbol = argOrOption(ctx, 1, 'a', ASSET_ARG);
                    if (assetSymbol == null) throw new IllegalArgumentException(INIT_ACCOUNT_ASSET_ARG_ERROR);
                    String balanceStr = argOrOption(ctx, 2, 'b', BALANCE_ARG);
                    if (balanceStr == null) throw new IllegalArgumentException(INIT_ACCOUNT_BALANCE_ARG_ERROR);

                    Long parentAccountId = parentId != null && !parentId.isBlank() ? Long.valueOf(parentId) : null;

                    Account account = accountService.createAccountWithOpeningBalance(accountName, parentAccountId, assetSymbol, balanceStr);
                    ctx.outputWriter().println("Account '" + account.getName() + "' created with opening balance " + balanceStr + " " + assetSymbol + ".");
                });
    }

    @Bean
    public Command listAccounts() {
        return Command.builder()
                .name("account list")
                .description(ACCOUNT_LIST_COMMAND_DESCRIPTION)
                .help(ACCOUNT_LIST_COMMAND_HELP)
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
                .description(ACCOUNT_FIND_COMMAND_DESCRIPTION)
                .help(ACCOUNT_FIND_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('t')
                                .longName(TYPE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('a')
                                .longName(ASSET_ARG)
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .execute(ctx -> {
                    String keyword = argOrOption(ctx, 0, 'k', KEYWORD_ARG);
                    String masterType = getOptionOrDefault(ctx, 't', TYPE_ARG, null);
                    String assetSymbol = getOptionOrDefault(ctx, 'a', ASSET_ARG, null);

                    List<Account> results = accountService.findAccounts(new AccountFilter(keyword, masterType, assetSymbol));

                    if (results.isEmpty()) {
                        ctx.outputWriter().println("No accounts found matching the given filters.");
                        return;
                    }

                    NumberFormat nf = NumberFormat.getNumberInstance();
                    nf.setMinimumFractionDigits(2);
                    nf.setMaximumFractionDigits(2);

                    ctx.outputWriter().printf("%-4s | %-40s | %-10s | %-10s | %-10s | %-18s | %-7s%n", "ID", "Name", "Type", "Asset", "Leaf", "Balance", "Internal");
                    ctx.outputWriter().printf("%-4s-|-%40s-|-%10s-|-%10s-|-%10s-|-%-18s-|-%-7s%n",
                            "----", "----------------------------------------", "----------", "----------", "----------", "------------------", "-------");
                    for (Account account : results) {
                        String assetName = account.getAsset() == null ? "—" : account.getAsset().getSymbol();
                        boolean isLeaf = !accountRepository.existsByParent_Id(account.getId());
                        BigDecimal balance = account.getAsset() == null
                                ? null
                                : transactionRepository.getAccountBalance(account.getId());
                        ctx.outputWriter().printf("%-4d | %-40s | %-10s | %-10s | %-10s | %-18s | %-7s%n",
                                account.getId(),
                                truncate(account.getName(), 40),
                                account.getMasterType(),
                                assetName,
                                isLeaf ? "yes" : "no",
                                balance == null ? "—" : nf.format(balance) + " " + assetName,
                                account.getIsInternal());
                    }
                });
    }

    @Bean
    public Command deleteAccount() {
        return Command.builder()
                .name("account delete")
                .description(DELETE_ACCOUNT_COMMAND_DESCRIPTION)
                .help(DELETE_ACCOUNT_COMMAND_HELP)
                .options(CommandOption.with()
                        .shortName('a')
                        .longName(ACCOUNT_ID_ARG)
                        .required(false)
                        .type(String.class)
                        .build())
                .exitStatusExceptionMapper(exceptionMapper())
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    String accountIdStr = argOrOption(ctx, 0, 'a', ACCOUNT_ID_ARG);

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
