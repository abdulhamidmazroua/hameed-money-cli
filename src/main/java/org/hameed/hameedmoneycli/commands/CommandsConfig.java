package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AccountType;
import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.model.dto.AccountCreateDto;
import org.hameed.hameedmoneycli.model.dto.AssetCreateDto;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.service.AccountService;
import org.hameed.hameedmoneycli.service.AssetService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandArgument;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CommandsConfig {

    private final ComponentFlow.Builder componentFlowBuilder;
    private final AssetService assetService;
    private final AccountService accountService;

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
                            .selectItems(List.of(
                                    SelectItem.of(AssetCategory.CASH.toString(), AssetCategory.CASH.toString()),
                                    SelectItem.of(AssetCategory.STOCK.toString(), AssetCategory.STOCK.toString()),
                                    SelectItem.of(AssetCategory.CRYPTO.toString(), AssetCategory.CRYPTO.toString()),
                                    SelectItem.of(AssetCategory.COMMODITY.toString(), AssetCategory.COMMODITY.toString()),
                                    SelectItem.of(AssetCategory.PROPERTY.toString(), AssetCategory.PROPERTY.toString())
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
                                .selectItems(List.of(
                                        SelectItem.of(AccountType.ASSET.toString(), AccountType.ASSET.toString()),
                                        SelectItem.of(AccountType.LIABILITY.toString(), AccountType.LIABILITY.toString()),
                                        SelectItem.of(AccountType.INCOME.toString(), AccountType.INCOME.toString()),
                                        SelectItem.of(AccountType.EXPENSE.toString(), AccountType.EXPENSE.toString())
                                )).and().build().run();

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
