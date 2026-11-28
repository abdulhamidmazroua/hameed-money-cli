package org.hameed.hameedmoneycomputer.market.provider;

import org.hameed.hameedmoneycomputer.market.AssetCategory;

import java.util.List;

public interface MarketDataProvider {

    List<MarketInstrument> getExchangeSymbols(String exchange, AssetCategory category);
}
