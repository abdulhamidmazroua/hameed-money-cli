package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.enums.StockExchange;
import org.hameed.hameedmoneycli.model.dto.AssetCreateDto;
import org.hameed.hameedmoneycli.model.dto.AssetFilter;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.service.AssetService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.util.List;

import static org.hameed.hameedmoneycli.util.CommandsUtil.*;
import static org.hameed.hameedmoneycli.constants.CommandConstants.*;

@Configuration
@RequiredArgsConstructor
public class AssetCommands {

    private final AssetService assetService;
    private final ComponentFlow.Builder componentFlowBuilder;

    @Bean
    public Command getCategories() {
        return Command.builder()
                .name("cat-list")
                .description(CAT_LIST_COMMAND_DESCRIPTION)
                .help(CAT_LIST_COMMAND_HELP)
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
                .description(ASSET_FETCH_COMMAND_DESCRIPTION)
                .help(ASSET_FETCH_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(CommandOption.with()
                                .shortName('c')
                                .longName(CATEGORY_ARG)
                                .type(String.class)
                                .required(false)
                                .build(),
                        CommandOption.with()
                                .shortName('e')
                                .longName(EXCHANGE_ARG)
                                .type(String.class)
                                .required(false)
                                .build())
                .execute(ctx -> {
                    String category = argOrOption(ctx, 0, 'c', CATEGORY_ARG);
                    if (category == null) throw new IllegalArgumentException(ASSET_FETCH_CATEGORY_ARG_ERROR);
                    AssetCategory assetCategory = AssetCategory.fromString(category);

                    if (assetCategory != AssetCategory.STOCK && assetCategory != AssetCategory.ETF && assetCategory != AssetCategory.MUTUAL_FUND) {
                        throw new IllegalArgumentException(String.format(ASSET_FETCH_UNSUPPORTED_CATEGORY, category));
                    }
                    String exchange = argOrOption(ctx, 1, 'e', EXCHANGE_ARG);
                    if (exchange == null) throw new IllegalArgumentException(ASSET_FETCH_EXCHANGE_ARG_ERROR);
                    assetService.syncAssetData(StockExchange.fromString(exchange), assetCategory);
                });
    }

    @Bean
    public Command registerAsset() {
        return Command.builder()
                .name("asset register")
                .description(ASSET_REGISTER_COMMAND_DESCRIPTION)
                .help(ASSET_REGISTER_COMMAND_HELP)
                .options(CommandOption.with()
                                .shortName('n')
                                .longName(NAME_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('s')
                                .longName(SYMBOL_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('c')
                                .longName(CATEGORY_ARG)
                                .required(false)
                                .type(String.class)
                                .build())
                .exitStatusExceptionMapper(exceptionMapper())
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    String assetName = argOrOption(ctx, 0, 'n', NAME_ARG);
                    if (assetName == null) throw new IllegalArgumentException(ASSET_REGISTER_NAME_ARG_ERROR);
                    String symbol = argOrOption(ctx, 1, 's', SYMBOL_ARG);
                    if (symbol == null) throw new IllegalArgumentException(ASSET_REGISTER_SYMBOL_ARG_ERROR);
                    String categoryArg = getOptionOrDefault(ctx, 'c', CATEGORY_ARG, null);

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
                    assetService.createAsset(new AssetCreateDto(assetName, symbol, AssetCategory.fromString(assetCategory)));
                });
    }

    @Bean
    public Command listAssets() {
        return Command.builder()
                .name("asset list")
                .description(ASSET_LIST_COMMAND_DESCRIPTION)
                .help(ASSET_LIST_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    List<Asset> assets = assetService.getAllAssets();
                    assets.forEach(asset -> ctx.outputWriter().println(asset.getName() + " (ID: " + asset.getId() + ") - Symbol: " + asset.getSymbol() + " - Category: " + asset.getCategory()));
                });
    }

    @Bean
    public Command findAssets() {
        return Command.builder()
                .name("asset find")
                .description(ASSET_FIND_COMMAND_DESCRIPTION)
                .help(ASSET_FIND_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('c')
                                .longName(CATEGORY_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .longName(TRADABLE_ARG)
                                .required(false)
                                .type(Boolean.class)
                                .build(),
                        CommandOption.with()
                                .longName(NON_TRADABLE_ARG)
                                .required(false)
                                .type(Boolean.class)
                                .build()
                )
                .execute(ctx -> {
                    String keyword = argOrOption(ctx, 0, 'k', KEYWORD_ARG);
                    String category = getOptionOrDefault(ctx, 'c', CATEGORY_ARG, null);
                    Boolean tradable = null;
                    if (getOptionOrDefault(ctx, (char) 0, TRADABLE_ARG, null) != null) {
                        tradable = true;
                    } else if (getOptionOrDefault(ctx, (char) 0, NON_TRADABLE_ARG, null) != null) {
                        tradable = false;
                    }

                    List<Asset> results = assetService.findAssets(new AssetFilter(keyword, category, tradable));

                    if (results.isEmpty()) {
                        ctx.outputWriter().println("No assets found matching the given filters.");
                        return;
                    }

                    ctx.outputWriter().printf("%-4s | %-35s | %-10s | %-12s | %s%n", "ID", "Name", "Symbol", "Category", "Tradable");
                    ctx.outputWriter().printf("%-4s-|-%35s-|-%10s-|-%12s-|-%s%n",
                            "----", "-----------------------------------", "----------", "------------", "--------");
                    for (Asset asset : results) {
                        ctx.outputWriter().printf("%-4d | %-35s | %-10s | %-12s | %s%n",
                                asset.getId(),
                                truncate(asset.getName(), 35),
                                asset.getSymbol(),
                                asset.getCategory().getCategory(),
                                asset.getIsTradable());
                    }
                });
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
