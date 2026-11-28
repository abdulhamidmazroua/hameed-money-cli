package org.hameed.hameedmoneycomputer.store;

import org.hameed.hameedmoneycomputer.market.MarketQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MarketQuoteRepository extends JpaRepository<MarketQuote, Long> {

    List<MarketQuote> findByBaseAsset_SymbolAndQuoteAsset_Symbol(String baseSymbol, String quoteSymbol);

   @Query(value = "SELECT mq.* FROM market_quotes mq INNER JOIN (SELECT base_asset_id, quote_asset_id, MAX(quote_date) AS max_date FROM market_quotes GROUP BY base_asset_id, quote_asset_id) latest ON mq.base_asset_id = latest.base_asset_id AND mq.quote_asset_id = latest.quote_asset_id AND mq.quote_date = latest.max_date", nativeQuery = true)
   List<MarketQuote> findAllLatest();

}
