package org.hameed.hameedmoneycomputer.provider;

import org.hameed.hameedmoneycomputer.enums.AssetCategory;

public record MarketInstrument(
        String symbol,
        String name,
        String currency,
        String exchange,
        AssetCategory category,
        String country,
        String isin
) {}
