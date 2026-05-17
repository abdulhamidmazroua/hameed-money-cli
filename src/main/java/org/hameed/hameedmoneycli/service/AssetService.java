package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.enums.StockExchange;
import org.hameed.hameedmoneycli.model.dto.AssetCreateDto;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.proxy.TwelveDataProxy;
import org.hameed.hameedmoneycli.proxy.dto.TwelveDataStockDto;
import org.hameed.hameedmoneycli.repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssetService {


    private final AssetRepository assetRepository;
    private final TwelveDataProxy twelveDataProxy;

    public void createAsset(AssetCreateDto newAsset) {
        Asset asset = Asset.builder()
                .name(newAsset.name())
                .symbol(newAsset.symbol())
                .category(newAsset.category())
                .isTradable(newAsset.isTradable())
                .build();
        assetRepository.save(asset);

    }

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
        return assetRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Asset with symbol " + symbol + " not found"));
    }

    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    public void syncAssetData(StockExchange stockExchange) {
        List<Map<String, String>> assetDataList = twelveDataProxy.getStockData(stockExchange.toString());

        assetDataList.forEach(assetData -> {
            Asset existingAsset = assetRepository.findBySymbol(assetData.get("symbol")).orElse(null);

            if (existingAsset != null) {
                existingAsset.setName(assetData.get("name"));
                existingAsset.setMetadata(assetData);
                assetRepository.save(existingAsset);
            } else {
                AssetCreateDto assetCreateDto = new AssetCreateDto(
                        assetData.get("name"),
                        assetData.get("symbol"),
                        AssetCategory.STOCK,
                        true
                );
                this.createAsset(assetCreateDto, assetData);
            }
        });
    }
}
