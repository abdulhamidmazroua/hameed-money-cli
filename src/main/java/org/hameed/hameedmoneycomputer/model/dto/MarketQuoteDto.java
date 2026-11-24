package org.hameed.hameedmoneycomputer.model.dto;

import java.math.BigDecimal;

public record MarketQuoteDto(
        String baseSymbol,
        String quoteSymbol,
        BigDecimal price,
        String marketQuoteDate
) {
}
