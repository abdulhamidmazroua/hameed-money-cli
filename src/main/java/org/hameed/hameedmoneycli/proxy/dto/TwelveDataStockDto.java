package org.hameed.hameedmoneycli.proxy.dto;

public record TwelveDataStockDto(
        String symbol,
        String name,
        String currency,
        String exchange,
        String minCode,
        String country,
        String type,
        String figiCode,
        String cfiCode,
        String isin,
        String cusio
) {
}
