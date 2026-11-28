package org.hameed.hameedmoneycomputer.ledger;

import java.util.Optional;

/**
 * Implemented by features that keep references to accounts, so that an account they still use can't be deleted.
 * Lets the ledger enforce this without depending on those features.
 */
public interface AccountReferences {

    /** Why {@code account} can't be deleted, or empty when this feature doesn't reference it. */
    Optional<String> blockingReason(Account account);
}
