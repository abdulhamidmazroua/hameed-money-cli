package org.hameed.hameedmoneycomputer.market;

import java.util.List;
import java.util.Optional;

/** Persistence port for assets (currencies, securities, commodities, property). */
public interface AssetStore {

    Asset save(Asset asset);

    Optional<Asset> findById(long id);

    /** First asset with this symbol. Symbols are unique per category, not globally. */
    Optional<Asset> findBySymbol(String symbol);

    Optional<Asset> findBySymbolAndCategory(String symbol, AssetCategory category);

    List<Asset> findAll();

    List<Asset> find(AssetFilter filter);
}
