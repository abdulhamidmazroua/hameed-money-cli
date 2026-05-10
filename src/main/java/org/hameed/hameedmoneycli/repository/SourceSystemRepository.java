package org.hameed.hameedmoneycli.repository;

import org.hameed.hameedmoneycli.model.entity.SourceSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SourceSystemRepository extends JpaRepository<SourceSystem, Long> {

    Optional<SourceSystem> findByCode(String code);
}
