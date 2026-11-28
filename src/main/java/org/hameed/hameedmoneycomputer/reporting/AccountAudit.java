package org.hameed.hameedmoneycomputer.reporting;

import org.hameed.hameedmoneycomputer.ledger.AccountView;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One account's balance and activity. The dates are null when it has no transactions. */
public record AccountAudit(
        AccountView account,
        BigDecimal balance,
        long transactionCount,
        LocalDate firstTransaction,
        LocalDate lastTransaction
) {
}
