package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.enums.StockExchange;
import org.hameed.hameedmoneycli.model.AssetSpecification;
import org.hameed.hameedmoneycli.model.dto.AssetCreateDto;
import org.hameed.hameedmoneycli.model.dto.AssetFilter;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.provider.MarketDataProvider;
import org.hameed.hameedmoneycli.provider.MarketInstrument;
import org.hameed.hameedmoneycli.repository.AssetRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final MarketDataProvider marketDataProvider;

    @Transactional
    public void createAsset(AssetCreateDto newAsset) {
        Asset asset = Asset.builder()
                .name(newAsset.name())
                .symbol(newAsset.symbol())
                .category(newAsset.category())
                .isTradable(newAsset.isTradable())
                .build();
        assetRepository.save(asset);
    }

    @Transactional
    public void createAsset(AssetCreateDto newAsset, Map<String, String> metadata) {
        Asset asset = Asset.builder()
                .name(newAsset.name())
                .symbol(newAsset.symbol())
                .category(newAsset.category())
                .isTradable(newAsset.isTradable())
                .build();
        asset.setMetadata(metadata);
        assetRepository.save(asset);
    }

    public Asset getAssetById(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset with ID " + id + " not found"));
    }

    public Asset getAssetBySymbol(String symbol) {
        return assetRepository.findFirstBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Asset with symbol " + symbol + " not found"));
    }

    public Asset getAssetBySymbolAndCategory(String symbol, AssetCategory category) {
        return assetRepository.findBySymbolAndCategory(symbol, category)
                .orElseThrow(() -> new IllegalArgumentException("Asset with symbol " + symbol + " and category " + category + " not found"));
    }

    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    public List<Asset> findAssets(AssetFilter filter) {
        return assetRepository.findAll(
                Specification
                        .where(AssetSpecification.hasNameOrSymbolContaining(filter.keyword()))
                        .and(AssetSpecification.hasCategory(filter.category()))
                        .and(AssetSpecification.hasTradable(filter.tradable()))
        );
    }

    @Transactional
    public void syncAssetData(StockExchange stockExchange, AssetCategory category) {
        List<MarketInstrument> instruments = marketDataProvider.getExchangeSymbols(stockExchange.toString(), category);

        instruments.forEach(instrument -> {
            Asset existingAsset = assetRepository.findBySymbolAndCategory(instrument.symbol(), category).orElse(null);

            Map<String, String> metadata = buildMetadata(instrument);

            if (existingAsset != null) {
                existingAsset.setName(instrument.name());
                existingAsset.setMetadata(metadata);
                assetRepository.save(existingAsset);
            } else {
                AssetCreateDto assetCreateDto = new AssetCreateDto(
                        instrument.name(),
                        instrument.symbol(),
                        instrument.category(),
                        true
                );
                this.createAsset(assetCreateDto, metadata);
            }
        });
    }

    private Map<String, String> buildMetadata(MarketInstrument instrument) {
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
