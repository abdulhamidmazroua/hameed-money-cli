package org.hameed.hameedmoneycomputer.store;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.market.Asset;
import org.hameed.hameedmoneycomputer.market.AssetCategory;
import org.hameed.hameedmoneycomputer.market.AssetFilter;
import org.hameed.hameedmoneycomputer.market.AssetStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class JpaAssetStore implements AssetStore {

    private final AssetRepository repository;

    @Override
    public Asset save(Asset asset) {
        return repository.save(asset);
    }

    @Override
    public Optional<Asset> findById(long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Asset> findBySymbol(String symbol) {
        return repository.findFirstBySymbol(symbol);
    }

    @Override
    public Optional<Asset> findBySymbolAndCategory(String symbol, AssetCategory category) {
        return repository.findBySymbolAndCategory(symbol, category);
    }

    @Override
    public List<Asset> findAll() {
        return repository.findAll();
    }

    @Override
    public List<Asset> find(AssetFilter filter) {
        return repository.findAll(AssetSpecification.matching(filter));
    }
}
