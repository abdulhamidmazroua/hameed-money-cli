package org.hameed.hameedmoneycli.enums;

public enum StockExchange {
    EGX,
    NASDAQ,
    NYSE,
    LSE,
    TSE,
    HKEX,
    ASX,
    TSX,
    BSE,
    NSE,
    TADAWUL,
    ADX,
    DFM,
    QE,
    EURONEXT,
    SSE,
    FWB,
    SIX,
    KRX,
    JPX;

    public static StockExchange fromString(String exchange) {
        for (StockExchange stockExchange : StockExchange.values()) {
            if (stockExchange.name().equalsIgnoreCase(exchange)) {
                return stockExchange;
            }
        }
        throw new IllegalArgumentException("No enum constant for exchange: " + exchange);
    }
}
