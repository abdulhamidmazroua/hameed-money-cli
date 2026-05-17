package org.hameed.hameedmoneycli.repository;

import org.hameed.hameedmoneycli.model.entity.MarketQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MarketQuoteRepository extends JpaRepository<MarketQuote, Long> {

    List<MarketQuote> findByBaseAsset_SymbolAndQuoteAsset_Symbol(String baseSymbol, String quoteSymbol);

   @Query(value = "SELECT mq.* FROM market_quote mq INNER JOIN (SELECT base_asset_id, quote_asset_id, MAX(quote_date) AS max_date FROM market_quote GROUP BY base_asset_id, quote_asset_id) latest ON mq.base_asset_id = latest.base_asset_id AND mq.quote_asset_id = latest.quote_asset_id AND mq.quote_date = latest.max_date", nativeQuery = true)
   List<MarketQuote> findAllLatest();

}
