package org.hameed.hameedmoneycomputer.market;

import java.util.List;

/** Persistence port for market quotes. */
public interface MarketQuoteStore {

    MarketQuote save(MarketQuote quote);

    List<MarketQuote> findByPair(String baseSymbol, String quoteSymbol);

    /** The most recent quote of every (base, quote) pair. */
    List<MarketQuote> findLatest();
}
