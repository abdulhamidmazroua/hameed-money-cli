package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.model.dto.AssetCreateDto;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetService {


    private final AssetRepository assetRepository;

    public void createAsset(AssetCreateDto newAsset) {
        Asset asset = Asset.builder()
                .name(newAsset.name())
                .symbol(newAsset.symbol())
                .category(newAsset.category())
                .isTradable(newAsset.isTradable())
                .build();
        assetRepository.save(asset);

    }

    public Asset getAssetById(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset with ID " + id + " not found"));
    }

    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }


}
