package org.hameed.hameedmoneycli.repository;

import org.hameed.hameedmoneycli.model.entity.MarketQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketQuoteRepository extends JpaRepository<MarketQuote, Long> {

    List<MarketQuote> findByBaseAsset_SymbolAndQuoteAsset_Symbol(String baseSymbol, String quoteSymbol);

}
