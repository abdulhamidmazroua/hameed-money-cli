package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.StockExchange;
import org.hameed.hameedmoneycli.model.dto.MarketQuoteDto;
import org.hameed.hameedmoneycli.model.entity.FinancialOracle;
import org.hameed.hameedmoneycli.model.entity.MarketQuote;
import org.hameed.hameedmoneycli.proxy.TwelveDataProxy;
import org.hameed.hameedmoneycli.repository.MarketQuoteRepository;
import org.hameed.hameedmoneycli.util.DateUtil;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.error.Mark;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketQuoteService {

    private final MarketQuoteRepository marketQuoteRepository;
    private final AssetService assetService;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Africa/Cairo");
    private final TwelveDataProxy twelveDataProxy;

    public void setMarketQuote(MarketQuoteDto marketQuote) {
        MarketQuote entity = MarketQuote.builder()
                .baseAsset(assetService.getAssetBySymbol(marketQuote.baseSymbol()))
                .quoteAsset(assetService.getAssetBySymbol(marketQuote.quoteSymbol()))
                .price(marketQuote.price())
                .quoteDate(marketQuote.marketQuoteDate() == null ? Instant.now() : DateUtil.parseDateStringToInstant(marketQuote.marketQuoteDate()))
                .build();

        marketQuoteRepository.save(entity);
    }

    public List<MarketQuoteDto> getMarketQuote(String baseSymbol, String quoteSymbol) {
        List<MarketQuote> marketQuotes = marketQuoteRepository.findByBaseAsset_SymbolAndQuoteAsset_Symbol(baseSymbol, quoteSymbol);
        return marketQuotes.stream()
                .map(mq -> new MarketQuoteDto(
                        mq.getBaseAsset().getSymbol(),
                        mq.getQuoteAsset().getSymbol(),
                        mq.getPrice(),
                        DateUtil.getDateStringFromInstant(mq.getQuoteDate())
                ))
                .toList();
    }

    public FinancialOracle getFinancialOracle() {
        FinancialOracle financialOracle = new FinancialOracle();
        marketQuoteRepository.findAllLatest()
                .forEach(financialOracle::addGraphNode);
        return financialOracle;
    }

}
