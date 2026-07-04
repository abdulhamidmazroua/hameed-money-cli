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

@Configuration
@RequiredArgsConstructor
public class AssetCommands {

    private final AssetService assetService;
    private final ComponentFlow.Builder componentFlowBuilder;

    @Bean
    public Command getCategories() {
        return Command.builder()
                .name("cat-list")
                .description("List all asset categories")
                .help("List all available asset categories (CASH, STOCK, ETF, CRYPTO, etc.). Usage: `cat-list`")
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
                .description("Fetch available securities from an exchange")
                .help("Fetch available securities (stocks, ETFs, funds) from an exchange. Usage: `asset fetch stock EGX` or `asset fetch --category stock --exchange EGX`")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
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
                .description("Register a new asset manually")
                .help("Register a new asset manually. Usage: `asset register \"Commercial International Bank\" COMI.CA` or `asset register --name \"Bank\" --symbol BNK`")
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
    public Command listAssets() {
        return Command.builder()
                .name("asset list")
                .description("List all registered assets")
                .help("List all registered assets with their ID, symbol and category. Usage: `asset list`")
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
                .description("Search assets by keyword, category, or tradable status")
                .help("Search assets by keyword (matches name or symbol), category, or tradable status. Usage: `asset find aapl` or `asset find --category stock --tradable` or `asset find` (lists all)")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('c')
                                .longName("category")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .longName("tradable")
                                .required(false)
                                .type(Boolean.class)
                                .build(),
                        CommandOption.with()
                                .longName("non-tradable")
                                .required(false)
                                .type(Boolean.class)
                                .build()
                )
                .execute(ctx -> {
                    String keyword = argOrOption(ctx, 0, 'k', "keyword");
                    String category = getOptionOrDefault(ctx, 'c', "category", null);
                    Boolean tradable = null;
                    if (getOptionOrDefault(ctx, (char) 0, "tradable", null) != null) {
                        tradable = true;
                    } else if (getOptionOrDefault(ctx, (char) 0, "non-tradable", null) != null) {
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
