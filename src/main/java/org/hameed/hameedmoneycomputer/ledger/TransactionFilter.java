package org.hameed.hameedmoneycomputer.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transaction search criteria; null fields are ignored. Dates are inclusive and interpreted in the app timezone.
 * The amount range matches when either side's amount falls inside it.
 */
public record TransactionFilter(
        TransactionType type,
        Long fromAccountId,
        Long toAccountId,
        LocalDate dateFrom,
        LocalDate dateTo,
        String description,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Long involvedAccountId
) {
    public TransactionFilter(TransactionType type, Long fromAccountId, Long toAccountId,
                             LocalDate dateFrom, LocalDate dateTo) {
        this(type, fromAccountId, toAccountId, dateFrom, dateTo, null, null, null, null);
    }
}
