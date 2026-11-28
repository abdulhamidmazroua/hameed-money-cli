package org.hameed.hameedmoneycomputer.ledger;

import java.math.BigDecimal;
import java.time.Instant;

/** A posted transaction as seen by clients. */
public record TransactionView(
        long id,
        Instant date,
        String description,
        TransactionType type,
        Leg from,
        Leg to,
        BigDecimal fee,
        String source,
        boolean systemAdjustment,
        Instant createdAt
) {
    /** One side of the transaction, in that account's asset. */
    public record Leg(long accountId, String accountName, String assetSymbol, BigDecimal amount) {}

    public static TransactionView from(Transaction t) {
        return new TransactionView(
                t.getId(),
                Instant.ofEpochMilli(t.getTransactionDate()),
                t.getDescription(),
                t.getType(),
                leg(t.getFromAccount(), t.getFromAmount()),
                leg(t.getToAccount(), t.getToAmount()),
                t.getFeeAmount() == null ? BigDecimal.ZERO : t.getFeeAmount(),
                t.getSourceSystem() == null ? null : t.getSourceSystem().getName(),
                Boolean.TRUE.equals(t.getIsSystemAdjustment()),
                t.getCreatedAt() == null ? null : Instant.ofEpochMilli(t.getCreatedAt()));
    }

    private static Leg leg(Account account, BigDecimal amount) {
        return new Leg(account.getId(), account.getName(),
                account.getAsset() == null ? null : account.getAsset().getSymbol(), amount);
    }
}
