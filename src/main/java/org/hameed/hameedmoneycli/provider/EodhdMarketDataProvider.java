package org.hameed.hameedmoneycli.provider;

import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.proxy.EodhdProxy;
import org.hameed.hameedmoneycli.proxy.dto.EodhdSymbolDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "hmc.market.data.provider.default", havingValue = "eodhd", matchIfMissing = true)
public class EodhdMarketDataProvider implements MarketDataProvider {

    private static final String TYPE_STOCK = "Common Stock";
    private static final String TYPE_PREFERRED = "Preferred Stock";
    private static final String TYPE_ETF = "ETF";
    private static final String TYPE_MUTUAL_FUND = "Mutual Fund";

    private final EodhdProxy eodhdProxy;

    public EodhdMarketDataProvider(EodhdProxy eodhdProxy) {
        this.eodhdProxy = eodhdProxy;
    }

    @Override
    public List<MarketInstrument> getExchangeSymbols(String exchange, AssetCategory category) {
        return eodhdProxy.getExchangeSymbols(exchange).stream()
                .filter(dto -> matchesCategory(dto.type(), category))
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
                dto.code(),
                dto.name(),
                dto.currency(),
                dto.exchange(),
                category,
                dto.country(),
                dto.isin()
        );
    }
}
