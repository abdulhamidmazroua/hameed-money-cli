package org.hameed.hameedmoneycli.model.dto;

import java.math.BigDecimal;

public record MarketQuoteDto(
        String baseSymbol,
        String quoteSymbol,
        BigDecimal price,
        String marketQuoteDate
) {
}
