package org.hameed.hameedmoneycli.provider;

import org.hameed.hameedmoneycli.enums.AssetCategory;

public record MarketInstrument(
        String symbol,
        String name,
        String currency,
        String exchange,
        AssetCategory category,
        String country,
        String isin
) {}
