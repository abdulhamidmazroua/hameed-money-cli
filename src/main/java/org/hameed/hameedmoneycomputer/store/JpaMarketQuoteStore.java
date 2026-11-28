package org.hameed.hameedmoneycomputer.store;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.market.MarketQuote;
import org.hameed.hameedmoneycomputer.market.MarketQuoteStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class JpaMarketQuoteStore implements MarketQuoteStore {

    private final MarketQuoteRepository repository;

    @Override
    public MarketQuote save(MarketQuote quote) {
        return repository.save(quote);
    }

    @Override
    public List<MarketQuote> findByPair(String baseSymbol, String quoteSymbol) {
        return repository.findByBaseAsset_SymbolAndQuoteAsset_Symbol(baseSymbol, quoteSymbol);
    }

    @Override
    public List<MarketQuote> findLatest() {
        return repository.findAllLatest();
    }
}
