package org.hameed.hameedmoneycomputer.reporting;

import java.math.BigDecimal;
import java.util.List;

/** Ledger-wide consistency checks: ASSET accounts with a negative balance and transactions without an account. */
public record LedgerAudit(
        int leafAccounts,
        long totalTransactions,
        List<NegativeBalance> negativeBalances,
        long orphanedFrom,
        long orphanedTo
) {
    public record NegativeBalance(long accountId, String accountName, BigDecimal balance) {}

    public long orphanedTotal() {
        return orphanedFrom + orphanedTo;
    }

    /** Each negative balance counts once; orphaned transactions count once in total. */
    public int anomalyCount() {
        return negativeBalances.size() + (orphanedTotal() > 0 ? 1 : 0);
    }
}
