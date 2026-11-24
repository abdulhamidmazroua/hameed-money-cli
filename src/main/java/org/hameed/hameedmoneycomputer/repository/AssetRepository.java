package org.hameed.hameedmoneycomputer.repository;

import org.hameed.hameedmoneycomputer.enums.AssetCategory;
import org.hameed.hameedmoneycomputer.model.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    Optional<Asset> findFirstBySymbol(String symbol);

    Optional<Asset> findBySymbolAndCategory(String symbol, AssetCategory category);
}
