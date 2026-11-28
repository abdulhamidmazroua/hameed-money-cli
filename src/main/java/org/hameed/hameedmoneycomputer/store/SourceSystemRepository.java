package org.hameed.hameedmoneycomputer.store;

import org.hameed.hameedmoneycomputer.ledger.SourceSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SourceSystemRepository extends JpaRepository<SourceSystem, Long> {

    Optional<SourceSystem> findByCode(String code);

    boolean existsByAnchoredAccount_Id(Long accountId);

    boolean existsByCode(String code);
}
