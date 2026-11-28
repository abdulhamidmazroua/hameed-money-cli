package org.hameed.hameedmoneycomputer.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persistence port for transactions, including the aggregate queries that balances and reports need. */
public interface TransactionStore {

    Transaction save(Transaction transaction);

    List<Transaction> find(TransactionFilter filter);

    long count();

    boolean existsByExternalRef(String externalRefId);

    /** True when the account is on either side of any transaction. */
    boolean involvesAccount(long accountId);

    long countForAccount(long accountId);

    /** Dates of the account's first and last transaction; empty when it has none. */
    Optional<TimeRange> dateRangeForAccount(long accountId);

    /** ASSET and EXPENSE: Σin − Σout. LIABILITY, INCOME and SYSTEM: Σout − Σin. */
    BigDecimal balanceOf(long accountId);

    /** Date of the earliest non-system transaction; empty when there is none. */
    Optional<Instant> firstActivityDate();

    List<Transaction> findSystemAdjustments();

    /** Σ fromAmount across all transactions. */
    BigDecimal totalVolume();

    /** Transactions whose from-account no longer exists. */
    long countOrphanedFrom();

    /** Transactions whose to-account no longer exists. */
    long countOrphanedTo();

    /**
     * Net outflow (Σfrom − Σto) per leaf account of {@code type}, for transactions dated in
     * [fromMillis, toExclusiveMillis). Accounts without transactions in the range are omitted.
     */
    List<AccountFlow> netOutflowByAccountType(AccountType type, long fromMillis, long toExclusiveMillis);

    record TimeRange(Instant first, Instant last) {}

    record AccountFlow(Account account, BigDecimal netOutflow) {}
}
