package org.hameed.hameedmoneycli.repository;

import org.hameed.hameedmoneycli.model.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

}
