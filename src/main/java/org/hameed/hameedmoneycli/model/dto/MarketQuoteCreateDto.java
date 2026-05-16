package org.hameed.hameedmoneycli.model.dto;

import java.math.BigDecimal;

public record MarketQuoteCreateDto(
        String baseSymbol,
        String quoteSymbol,
        BigDecimal price
) {
}
