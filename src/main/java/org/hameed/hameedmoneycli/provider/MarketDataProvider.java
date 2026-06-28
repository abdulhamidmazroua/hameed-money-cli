package org.hameed.hameedmoneycli.provider;

import org.hameed.hameedmoneycli.enums.AssetCategory;

import java.util.List;

public interface MarketDataProvider {

    List<MarketInstrument> getExchangeSymbols(String exchange, AssetCategory category);
}
