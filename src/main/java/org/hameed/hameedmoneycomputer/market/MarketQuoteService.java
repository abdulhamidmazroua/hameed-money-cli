package org.hameed.hameedmoneycomputer.market;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.market.provider.YahooFinanceProxy;
import org.hameed.hameedmoneycomputer.shared.DateUtil;
import org.hameed.hameedmoneycomputer.shared.ExternalServiceException;
import org.hameed.hameedmoneycomputer.shared.NotFoundException;
import org.hameed.hameedmoneycomputer.shared.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Market quotes (manual or fetched) and the {@link FinancialOracle} built from them. */
@Service
@RequiredArgsConstructor
public class MarketQuoteService {

    private static final Logger log = LoggerFactory.getLogger(MarketQuoteService.class);

    private final MarketQuoteStore quotes;
    private final AssetStore assets;
    private final YahooFinanceProxy yahooFinanceProxy;

    /** Stores a quote; a null date means now. */
    @Transactional
    public MarketQuoteDto setMarketQuote(MarketQuoteDto marketQuote) {
        if (marketQuote.price() == null || marketQuote.price().signum() <= 0) {
            throw new ValidationException("Price must be greater than zero");
        }
        MarketQuote saved = quotes.save(MarketQuote.builder()
                .baseAsset(requireAsset(marketQuote.baseSymbol()))
                .quoteAsset(requireAsset(marketQuote.quoteSymbol()))
                .price(marketQuote.price())
                .quoteDate(marketQuote.date() == null
                        ? System.currentTimeMillis()
                        : DateUtil.startOfDayMillis(marketQuote.date()))
                .build());
        return toDto(saved);
    }

    public List<MarketQuoteDto> getMarketQuote(String baseSymbol, String quoteSymbol) {
        return quotes.findByPair(baseSymbol, quoteSymbol).stream().map(MarketQuoteService::toDto).toList();
    }

    /** The latest quote of every pair. */
    public List<MarketQuoteDto> listMarketQuotes() {
        return quotes.findLatest().stream().map(MarketQuoteService::toDto).toList();
    }

    /** Converts between any two assets connected by the latest quotes. */
    public FinancialOracle getFinancialOracle() {
        FinancialOracle financialOracle = new FinancialOracle();
        quotes.findLatest().forEach(financialOracle::addGraphNode);
        return financialOracle;
    }

    @Transactional
    public MarketQuoteDto fetchAndSaveQuote(String baseSymbol, String quoteSymbol) {
        Asset base = requireAsset(baseSymbol);
        Asset quote = requireAsset(quoteSymbol);

        String yahooSymbol = resolveYahooSymbol(base, quote);

        BigDecimal price = yahooFinanceProxy.fetchPrice(yahooSymbol)
                .orElseThrow(() -> new ExternalServiceException(
                        "Could not fetch price for " + yahooSymbol + " from Yahoo Finance. " +
                        "Verify the symbol is correct, or use `quote set` to set it manually."
                ));

        return setMarketQuote(new MarketQuoteDto(baseSymbol, quoteSymbol, price, null));
    }

    /** Re-fetches every pair that has a stored quote; failures are collected, not thrown. */
    public QuoteRefreshResult refreshAllQuotes() {
        List<String> updated = new ArrayList<>();
        List<QuoteRefreshResult.Failure> failed = new ArrayList<>();

        for (MarketQuote quote : quotes.findLatest()) {
            String baseSymbol = quote.getBaseAsset().getSymbol();
            String quoteSymbol = quote.getQuoteAsset().getSymbol();
            try {
                fetchAndSaveQuote(baseSymbol, quoteSymbol);
                updated.add(baseSymbol + " -> " + quoteSymbol);
            } catch (Exception e) {
                failed.add(new QuoteRefreshResult.Failure(baseSymbol, quoteSymbol, e.getMessage()));
            }
        }

        return new QuoteRefreshResult(updated, failed);
    }

    private Asset requireAsset(String symbol) {
        return assets.findBySymbol(symbol)
                .orElseThrow(() -> new NotFoundException("Asset with symbol " + symbol + " not found"));
    }

    private static MarketQuoteDto toDto(MarketQuote quote) {
        return new MarketQuoteDto(
                quote.getBaseAsset().getSymbol(),
                quote.getQuoteAsset().getSymbol(),
                quote.getPrice(),
                DateUtil.toLocalDate(Instant.ofEpochMilli(quote.getQuoteDate())));
    }

    private String resolveYahooSymbol(Asset base, Asset quote) {
        AssetCategory baseCat = base.getCategory();
        AssetCategory quoteCat = quote.getCategory();

        if (baseCat.isSecurity() && quoteCat == AssetCategory.CASH) {
            String nativeCurrency = metadata(base, "currency");
            if (nativeCurrency != null && !nativeCurrency.equalsIgnoreCase(quote.getSymbol())) {
                log.warn("{} trades in {}, not {}. The Oracle can convert to {}.",
                        base.getSymbol(), nativeCurrency, quote.getSymbol(), quote.getSymbol());
            }
            String isin = metadata(base, "isin");
            if (isin != null) {
                log.info("Resolving {} via ISIN {}", base.getSymbol(), isin);
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

        if (baseCat == AssetCategory.CASH && quoteCat.isSecurity()) {
            throw new ValidationException(
                    "Cannot fetch CASH -> " + quote.getSymbol() + " (" + quoteCat + ") directly. " +
                    "Try: `quote fetch --base " + quote.getSymbol() + " --quote " + base.getSymbol() + "`"
            );
        }

        if (baseCat == AssetCategory.PROPERTY || quoteCat == AssetCategory.PROPERTY) {
            throw new ValidationException("Property assets require manual quotes. Use `quote set`.");
        }

        throw new ValidationException(
                "Cannot fetch a quote from " + baseCat + " to " + quoteCat + ". Use a cash intermediary.");
    }

    private String buildYahooSymbol(Asset asset) {
        String symbol = asset.getSymbol();
        String exchangeName = metadata(asset, "exchange");
        if (exchangeName == null) {
            return symbol;
        }
        try {
            String suffix = StockExchange.fromString(exchangeName).getYahooSuffix();
            if (!suffix.isEmpty() && !symbol.endsWith(suffix)) {
                return symbol + suffix;
            }
        } catch (ValidationException e) {
            // exchange not mapped in the enum — use the symbol as-is
        }
        return symbol;
    }

    private static String metadata(Asset asset, String key) {
        return asset.getMetadata() == null ? null : asset.getMetadata().get(key);
    }
}
