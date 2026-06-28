package org.hameed.hameedmoneycli.provider;

import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.proxy.TwelveDataProxy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "hmc.market.data.provider.default", havingValue = "twelvedata")
public class TwelveDataMarketDataProvider implements MarketDataProvider {

    private final TwelveDataProxy twelveDataProxy;

    public TwelveDataMarketDataProvider(TwelveDataProxy twelveDataProxy) {
        this.twelveDataProxy = twelveDataProxy;
    }

    @Override
    public List<MarketInstrument> getExchangeSymbols(String exchange, AssetCategory category) {
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

    private MarketInstrument toMarketInstrument(Map<String, String> raw) {
        return new MarketInstrument(
                raw.get("symbol"),
                raw.get("name"),
                raw.get("currency"),
                raw.get("exchange"),
                AssetCategory.STOCK,
                raw.get("country"),
                raw.get("isin")
        );
    }
}
