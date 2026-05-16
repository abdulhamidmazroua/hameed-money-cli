package org.hameed.hameedmoneycli.repository;

import org.hameed.hameedmoneycli.model.entity.MarketQuote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketQuoteRepository extends JpaRepository<MarketQuote, Long> {

}
