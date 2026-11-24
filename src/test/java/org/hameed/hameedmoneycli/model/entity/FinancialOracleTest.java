package org.hameed.hameedmoneycli.model.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FinancialOracleTest {

    private static Asset asset(long id) {
        Asset a = new Asset();
        a.setId(id);
        return a;
    }

    private static MarketQuote quote(long base, long quote, String price) {
        return MarketQuote.builder().baseAsset(asset(base)).quoteAsset(asset(quote)).price(new BigDecimal(price)).build();
    }

    private static void assertRate(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.setScale(6, java.math.RoundingMode.HALF_UP)),
                () -> "expected " + expected + " but was " + actual);
    }

    @Test
    void sameAssetIsOne() {
        assertEquals(BigDecimal.ONE, new FinancialOracle().getRate(1L, 1L));
    }

    @Test
    void directAndInverseRates() {
        FinancialOracle oracle = new FinancialOracle();
        oracle.addGraphNode(quote(1, 2, "50"));

        assertRate("50", oracle.getRate(1L, 2L));
        assertRate("0.02", oracle.getRate(2L, 1L));
    }

    @Test
    void multiHopConversion() {
        // stock(3) priced in 1, and 1 priced in 2  →  stock in 2 = 10 * 50
        FinancialOracle oracle = new FinancialOracle();
        oracle.addGraphNode(quote(3, 1, "10"));
        oracle.addGraphNode(quote(1, 2, "50"));

        assertRate("500", oracle.getRate(3L, 2L));
        assertRate("0.002", oracle.getRate(2L, 3L));
    }

    @Test
    void missingPathThrows() {
        FinancialOracle oracle = new FinancialOracle();
        oracle.addGraphNode(quote(1, 2, "50"));

        assertThrows(IllegalArgumentException.class, () -> oracle.getRate(1L, 9L));
    }
}
