package org.hameed.hameedmoneycomputer.store;

import org.hameed.hameedmoneycomputer.market.Asset;
import org.hameed.hameedmoneycomputer.market.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    Optional<Asset> findFirstBySymbol(String symbol);

    Optional<Asset> findBySymbolAndCategory(String symbol, AssetCategory category);
}
