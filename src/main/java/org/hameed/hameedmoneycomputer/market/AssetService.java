package org.hameed.hameedmoneycomputer.market;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.market.provider.MarketDataProvider;
import org.hameed.hameedmoneycomputer.market.provider.MarketInstrument;
import org.hameed.hameedmoneycomputer.shared.BusinessRuleException;
import org.hameed.hameedmoneycomputer.shared.NotFoundException;
import org.hameed.hameedmoneycomputer.shared.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The asset catalog: currencies, securities, commodities and property. */
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetStore assets;
    private final MarketDataProvider marketDataProvider;

    @Transactional
    public AssetView createAsset(AssetCreateDto newAsset) {
        if (newAsset.name() == null || newAsset.name().isBlank() || newAsset.symbol() == null || newAsset.symbol().isBlank()) {
            throw new ValidationException("Asset name and symbol are required");
        }
        if (newAsset.category() == null) {
            throw new ValidationException("Asset category is required");
        }
        if (assets.findBySymbolAndCategory(newAsset.symbol(), newAsset.category()).isPresent()) {
            throw new BusinessRuleException("Asset " + newAsset.symbol() + " (" + newAsset.category().getCategory() + ") already exists");
        }
        return AssetView.from(assets.save(newAsset(newAsset, null)));
    }

    /** The first asset with this symbol (symbols are unique per category). */
    public AssetView getAssetBySymbol(String symbol) {
        return assets.findBySymbol(symbol)
                .map(AssetView::from)
                .orElseThrow(() -> new NotFoundException("Asset with symbol " + symbol + " not found"));
    }

    public List<AssetView> getAllAssets() {
        return assets.findAll().stream().map(AssetView::from).toList();
    }

    public List<AssetView> findAssets(AssetFilter filter) {
        return assets.find(filter).stream().map(AssetView::from).toList();
    }

    /** Imports or updates every listed instrument of {@code category} on {@code stockExchange}; returns the count. */
    @Transactional
    public int syncAssetData(StockExchange stockExchange, AssetCategory category) {
        List<MarketInstrument> instruments = marketDataProvider.getExchangeSymbols(stockExchange.toString(), category);

        for (MarketInstrument instrument : instruments) {
            Map<String, String> metadata = buildMetadata(instrument);
            Asset existing = assets.findBySymbolAndCategory(instrument.symbol(), category).orElse(null);
            if (existing != null) {
                existing.setName(instrument.name());
                existing.setMetadata(metadata);
                assets.save(existing);
            } else {
                assets.save(newAsset(new AssetCreateDto(instrument.name(), instrument.symbol(), instrument.category()), metadata));
            }
        }
        return instruments.size();
    }

    private static Asset newAsset(AssetCreateDto dto, Map<String, String> metadata) {
        Asset asset = Asset.builder()
                .name(dto.name())
                .symbol(dto.symbol())
                .category(dto.category())
                .isTradable(dto.category().isTradable())
                .build();
        asset.setMetadata(metadata);
        return asset;
    }

    private static Map<String, String> buildMetadata(MarketInstrument instrument) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("symbol", instrument.symbol());
        metadata.put("name", instrument.name());
        metadata.put("currency", instrument.currency());
        metadata.put("exchange", instrument.exchange());
        metadata.put("country", instrument.country());
        metadata.put("isin", instrument.isin());
        return metadata;
    }
}
