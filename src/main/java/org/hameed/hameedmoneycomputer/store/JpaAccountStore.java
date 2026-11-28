package org.hameed.hameedmoneycomputer.store;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.ledger.Account;
import org.hameed.hameedmoneycomputer.ledger.AccountFilter;
import org.hameed.hameedmoneycomputer.ledger.AccountStore;
import org.hameed.hameedmoneycomputer.ledger.AccountType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class JpaAccountStore implements AccountStore {

    private final AccountRepository repository;

    @Override
    public Account save(Account account) {
        return repository.save(account);
    }

    @Override
    public void delete(Account account) {
        repository.delete(account);
    }

    @Override
    public Optional<Account> findById(long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Account> findByName(String name) {
        return repository.findByNameIgnoreCase(name);
    }

    @Override
    public List<Account> findAll() {
        return repository.findAll();
    }

    @Override
    public List<Account> find(AccountFilter filter) {
        return repository.findAll(AccountSpecification.matching(filter));
    }

    @Override
    public List<Account> findLeaves() {
        return repository.getLeafAccounts();
    }

    @Override
    public boolean hasChildren(long accountId) {
        return repository.existsByParent_Id(accountId);
    }

    @Override
    public boolean existsByAssetAndType(long assetId, AccountType type) {
        return repository.existsByAsset_IdAndMasterType(assetId, type);
    }
}
