package org.hameed.hameedmoneycli.enums;

import lombok.Getter;

@Getter
public enum StockExchange {
    EGX(".CA"),
    NASDAQ(""),
    NYSE(""),
    LSE(".L"),
    TSE(".T"),
    HKEX(".HK"),
    ASX(".AX"),
    TSX(".TO"),
    BSE(".BO"),
    NSE(".NS"),
    TADAWUL(".SR"),
    ADX(".AE"),
    DFM(".AE"),
    QE(".QA"),
    EURONEXT(".PA"),
    SSE(".SS"),
    FWB(".DE"),
    SIX(".SW"),
    KRX(".KS"),
    JPX(".T");

    private final String yahooSuffix;

    StockExchange(String yahooSuffix) {
        this.yahooSuffix = yahooSuffix;
    }

    public static StockExchange fromString(String exchange) {
        for (StockExchange stockExchange : StockExchange.values()) {
            if (stockExchange.name().equalsIgnoreCase(exchange)) {
                return stockExchange;
            }
        }
        throw new IllegalArgumentException("No enum constant for exchange: " + exchange);
    }
}
