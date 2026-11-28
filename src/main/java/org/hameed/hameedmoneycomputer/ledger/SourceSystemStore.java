package org.hameed.hameedmoneycomputer.ledger;

import java.util.List;
import java.util.Optional;

/** Persistence port for transaction sources. */
public interface SourceSystemStore {

    SourceSystem save(SourceSystem source);

    void delete(SourceSystem source);

    Optional<SourceSystem> findByCode(String code);

    List<SourceSystem> findAll();

    boolean existsByCode(String code);

    boolean isAnchoredTo(long accountId);
}
