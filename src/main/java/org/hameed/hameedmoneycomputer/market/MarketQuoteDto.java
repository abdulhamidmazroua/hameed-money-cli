package org.hameed.hameedmoneycomputer.market;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One {@code baseSymbol} is worth {@code price} {@code quoteSymbol} on {@code date}.
 * When setting a quote, a null date means now.
 */
public record MarketQuoteDto(
        String baseSymbol,
        String quoteSymbol,
        BigDecimal price,
        LocalDate date
) {
}
