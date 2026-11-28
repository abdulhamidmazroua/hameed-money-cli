package org.hameed.hameedmoneycomputer.store;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.ledger.SourceSystem;
import org.hameed.hameedmoneycomputer.ledger.SourceSystemStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class JpaSourceSystemStore implements SourceSystemStore {

    private final SourceSystemRepository repository;

    @Override
    public SourceSystem save(SourceSystem source) {
        return repository.save(source);
    }

    @Override
    public void delete(SourceSystem source) {
        repository.delete(source);
    }

    @Override
    public Optional<SourceSystem> findByCode(String code) {
        return repository.findByCode(code);
    }

    @Override
    public List<SourceSystem> findAll() {
        return repository.findAll();
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public boolean isAnchoredTo(long accountId) {
        return repository.existsByAnchoredAccount_Id(accountId);
    }
}
