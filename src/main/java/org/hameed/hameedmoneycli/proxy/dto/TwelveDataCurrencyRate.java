package org.hameed.hameedmoneycli.proxy.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TwelveDataCurrencyRate(
        String symbol,
        BigDecimal rate,
        Instant timestamp
) {

}
