package org.hameed.hameedmoneycli.provider;

import org.hameed.hameedmoneycli.config.HmcConfig;
import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.proxy.EodhdProxy;
import org.hameed.hameedmoneycli.proxy.TwelveDataProxy;
import org.hameed.hameedmoneycli.proxy.dto.EodhdSymbolDto;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Primary
public class MarketDataProviderImpl implements MarketDataProvider {

    private static final String TYPE_STOCK = "Common Stock";
    private static final String TYPE_PREFERRED = "Preferred Stock";
    private static final String TYPE_ETF = "ETF";
    private static final String TYPE_MUTUAL_FUND = "Mutual Fund";

    private final HmcConfig config;
    private final EodhdProxy eodhdProxy;
    private final TwelveDataProxy twelveDataProxy;

    public MarketDataProviderImpl(HmcConfig config, EodhdProxy eodhdProxy, TwelveDataProxy twelveDataProxy) {
        this.config = config;
        this.eodhdProxy = eodhdProxy;
        this.twelveDataProxy = twelveDataProxy;
    }

    @Override
    public List<MarketInstrument> getExchangeSymbols(String exchange, AssetCategory category) {
        return switch (config.getMarketDataProvider()) {
            case "eodhd" -> fetchFromEodhd(exchange, category);
            case "twelvedata" -> fetchFromTwelveData(exchange, category);
            default -> throw new IllegalArgumentException(
                    "Unknown market data provider \"" + config.getMarketDataProvider()
                    + "\". Set \"marketDataProvider\": \"eodhd\" or \"twelvedata\" in ~/.hmc/config.json"
            );
        };
    }

    private List<MarketInstrument> fetchFromEodhd(String exchange, AssetCategory category) {
        return eodhdProxy.getExchangeSymbols(exchange).stream()
                .filter(dto -> matchesCategory(dto.type(), category))
                .map(this::toMarketInstrument)
                .toList();
    }

    private List<MarketInstrument> fetchFromTwelveData(String exchange, AssetCategory category) {
        if (category != AssetCategory.STOCK) {
            throw new IllegalArgumentException(
                    "TwelveData only supports STOCK category. Use provider=eodhd for ETFs and mutual funds."
            );
        }
        List<Map<String, String>> rawData = twelveDataProxy.getExchangeSymbols(exchange);
        return rawData.stream()
                .map(this::toMarketInstrument)
                .toList();
    }

    private boolean matchesCategory(String type, AssetCategory category) {
        if (type == null) return false;
        return switch (category) {
            case STOCK -> type.equals(TYPE_STOCK) || type.equals(TYPE_PREFERRED);
            case ETF -> type.equals(TYPE_ETF);
            case MUTUAL_FUND -> type.equals(TYPE_MUTUAL_FUND);
            default -> false;
        };
    }

    private MarketInstrument toMarketInstrument(EodhdSymbolDto dto) {
        AssetCategory category;
        if (dto.type().equals(TYPE_STOCK) || dto.type().equals(TYPE_PREFERRED)) {
            category = AssetCategory.STOCK;
        } else if (dto.type().equals(TYPE_ETF)) {
            category = AssetCategory.ETF;
        } else if (dto.type().equals(TYPE_MUTUAL_FUND)) {
            category = AssetCategory.MUTUAL_FUND;
        } else {
            category = null;
        }
        return new MarketInstrument(
                dto.code(), dto.name(), dto.currency(), dto.exchange(),
                category, dto.country(), dto.isin()
        );
    }

    private MarketInstrument toMarketInstrument(Map<String, String> raw) {
        return new MarketInstrument(
                raw.get("symbol"), raw.get("name"), raw.get("currency"),
                raw.get("exchange"), AssetCategory.STOCK,
                raw.get("country"), raw.get("isin")
        );
    }
}
