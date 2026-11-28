package org.hameed.hameedmoneycomputer.ledger;

import java.util.List;
import java.util.Optional;

/** Persistence port for accounts. Implemented by the storage adapter. */
public interface AccountStore {

    Account save(Account account);

    void delete(Account account);

    Optional<Account> findById(long id);

    /** Exact name match, ignoring case. */
    Optional<Account> findByName(String name);

    List<Account> findAll();

    List<Account> find(AccountFilter filter);

    /** Accounts that hold an asset and can post transactions; folders are excluded. */
    List<Account> findLeaves();

    boolean hasChildren(long accountId);

    boolean existsByAssetAndType(long assetId, AccountType type);
}
