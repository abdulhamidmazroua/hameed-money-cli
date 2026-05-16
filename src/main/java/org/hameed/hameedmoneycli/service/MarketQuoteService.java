package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.model.dto.MarketQuoteCreateDto;
import org.hameed.hameedmoneycli.model.entity.MarketQuote;
import org.hameed.hameedmoneycli.repository.AssetRepository;
import org.hameed.hameedmoneycli.repository.MarketQuoteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MarketQuoteService {

    private final MarketQuoteRepository marketQuoteRepository;
    private final AssetService assetService;


    public void setMarketQuote(MarketQuoteCreateDto marketQuote) {
        MarketQuote entity = MarketQuote.builder()
                .baseAsset(assetService.getAssetBySymbol(marketQuote.baseSymbol()))
                .quoteAsset(assetService.getAssetBySymbol(marketQuote.quoteSymbol()))
                .price(marketQuote.price())
                .build();

        marketQuoteRepository.save(entity);
    }
}
