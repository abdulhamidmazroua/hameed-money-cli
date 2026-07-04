package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.enums.StockExchange;
import org.hameed.hameedmoneycli.model.dto.MarketQuoteDto;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.model.entity.FinancialOracle;
import org.hameed.hameedmoneycli.model.entity.MarketQuote;
import org.hameed.hameedmoneycli.proxy.YahooFinanceProxy;
import org.hameed.hameedmoneycli.repository.MarketQuoteRepository;
import org.hameed.hameedmoneycli.util.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketQuoteService {

    private final MarketQuoteRepository marketQuoteRepository;
    private final AssetService assetService;
    private final YahooFinanceProxy yahooFinanceProxy;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Africa/Cairo");

    @Transactional
    public void setMarketQuote(MarketQuoteDto marketQuote) {
        MarketQuote entity = MarketQuote.builder()
                .baseAsset(assetService.getAssetBySymbol(marketQuote.baseSymbol()))
                .quoteAsset(assetService.getAssetBySymbol(marketQuote.quoteSymbol()))
                .price(marketQuote.price())
                .quoteDate(marketQuote.marketQuoteDate() == null ? Instant.now() : DateUtil.parseDateStringToInstant(marketQuote.marketQuoteDate()))
                .build();

        marketQuoteRepository.save(entity);
    }

    public List<MarketQuoteDto> getMarketQuote(String baseSymbol, String quoteSymbol) {
        List<MarketQuote> marketQuotes = marketQuoteRepository.findByBaseAsset_SymbolAndQuoteAsset_Symbol(baseSymbol, quoteSymbol);
        return marketQuotes.stream()
                .map(mq -> new MarketQuoteDto(
                        mq.getBaseAsset().getSymbol(),
                        mq.getQuoteAsset().getSymbol(),
                        mq.getPrice(),
                        DateUtil.getDateStringFromInstant(mq.getQuoteDate())
                ))
                .toList();
    }

    public FinancialOracle getFinancialOracle() {
        FinancialOracle financialOracle = new FinancialOracle();
        marketQuoteRepository.findAllLatest()
                .forEach(financialOracle::addGraphNode);
        return financialOracle;
    }

    public List<MarketQuoteDto> listMarketQuotes() {
        return marketQuoteRepository.findAllLatest().stream()
                .map(mq -> new MarketQuoteDto(
                        mq.getBaseAsset().getSymbol(),
                        mq.getQuoteAsset().getSymbol(),
                        mq.getPrice(),
                        DateUtil.getDateStringFromInstant(mq.getQuoteDate())
                ))
                .toList();
    }

    @Transactional
    public void fetchAndSaveQuote(String baseSymbol, String quoteSymbol) {
        Asset base = assetService.getAssetBySymbol(baseSymbol);
        Asset quote = assetService.getAssetBySymbol(quoteSymbol);

        String yahooSymbol = resolveYahooSymbol(base, quote);

        BigDecimal price = yahooFinanceProxy.fetchPrice(yahooSymbol)
                .orElseThrow(() -> new IllegalStateException(
                        "Could not fetch price for " + yahooSymbol + " from Yahoo Finance. " +
                        "Verify the symbol is correct, or use `quote set` to set it manually."
                ));

        setMarketQuote(new MarketQuoteDto(baseSymbol, quoteSymbol, price, null));
    }

    private String resolveYahooSymbol(Asset base, Asset quote) {
        AssetCategory baseCat = base.getCategory();
        AssetCategory quoteCat = quote.getCategory();

        if (isSecurity(baseCat) && quoteCat == AssetCategory.CASH) {
            String nativeCurrency = extractNativeCurrency(base);
            if (nativeCurrency != null && !nativeCurrency.equalsIgnoreCase(quote.getSymbol())) {
                System.out.println("Warning: " + base.getSymbol() + " trades in " + nativeCurrency +
                        ", not " + quote.getSymbol() + ". The Oracle can convert to " + quote.getSymbol() + ".");
            }
            String isin = extractIsin(base);
            if (isin != null) {
                System.out.println("Resolving " + base.getSymbol() + " via ISIN " + isin);
                return isin;
            }
            return buildYahooSymbol(base);
        }

        if (baseCat == AssetCategory.CASH && quoteCat == AssetCategory.CASH) {
            return base.getSymbol() + quote.getSymbol() + "=X";
        }

        if (baseCat == AssetCategory.CRYPTO && quoteCat == AssetCategory.CASH) {
            return base.getSymbol() + "-" + quote.getSymbol();
        }

        if (baseCat == AssetCategory.COMMODITY && quoteCat == AssetCategory.CASH) {
            return base.getSymbol() + "=F";
        }

        if (baseCat == AssetCategory.CASH && isSecurity(quoteCat)) {
            throw new IllegalArgumentException(
                    "Cannot fetch CASH -> " + quote.getSymbol() + " (" + quoteCat + ") directly. " +
                    "Try: `quote fetch --base " + quote.getSymbol() + " --quote " + base.getSymbol() + "`"
            );
        }

        if (baseCat == AssetCategory.PROPERTY || quoteCat == AssetCategory.PROPERTY) {
            throw new IllegalArgumentException(
                    "Property assets require manual quotes. Use `quote set`."
            );
        }

        throw new IllegalArgumentException(
                "Cannot fetch a quote from " + baseCat + " to " + quoteCat + ". " +
                "Use a cash intermediary."
        );
    }

    private String buildYahooSymbol(Asset asset) {
        String symbol = asset.getSymbol();
        String exchangeName = extractExchangeName(asset);
        if (exchangeName == null) {
            return symbol;
        }
        try {
            StockExchange exchange = StockExchange.fromString(exchangeName);
            String suffix = exchange.getYahooSuffix();
            if (!suffix.isEmpty() && !symbol.endsWith(suffix)) {
                return symbol + suffix;
            }
        } catch (IllegalArgumentException e) {
            // exchange not mapped in enum — use symbol as-is
        }
        return symbol;
    }

    private String extractExchangeName(Asset asset) {
        if (asset.getMetadata() != null) {
            return asset.getMetadata().get("exchange");
        }
        return null;
    }

    private boolean isSecurity(AssetCategory category) {
        return category == AssetCategory.STOCK || category == AssetCategory.ETF || category == AssetCategory.MUTUAL_FUND;
    }

    private String extractNativeCurrency(Asset asset) {
        if (asset.getMetadata() != null) {
            return asset.getMetadata().get("currency");
        }
        return null;
    }

    private String extractIsin(Asset asset) {
        if (asset.getMetadata() != null) {
            return asset.getMetadata().get("isin");
        }
        return null;
    }
}
