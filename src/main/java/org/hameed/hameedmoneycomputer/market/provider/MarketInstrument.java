package org.hameed.hameedmoneycomputer.market.provider;

import org.hameed.hameedmoneycomputer.market.AssetCategory;

public record MarketInstrument(
        String symbol,
        String name,
        String currency,
        String exchange,
        AssetCategory category,
        String country,
        String isin
) {}
