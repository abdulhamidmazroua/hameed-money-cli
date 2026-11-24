package org.hameed.hameedmoneycomputer.provider;

import org.hameed.hameedmoneycomputer.enums.AssetCategory;

import java.util.List;

public interface MarketDataProvider {

    List<MarketInstrument> getExchangeSymbols(String exchange, AssetCategory category);
}
