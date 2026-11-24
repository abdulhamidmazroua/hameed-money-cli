package org.hameed.hameedmoneycomputer.repository;

import org.hameed.hameedmoneycomputer.model.entity.SourceSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SourceSystemRepository extends JpaRepository<SourceSystem, Long> {

    Optional<SourceSystem> findByCode(String code);

    boolean existsByAnchoredAccount_Id(Long accountId);

    boolean existsByCode(String code);
}
