package org.hameed.hameedmoneycomputer.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A manual transaction. {@code fromAmount} is in the from-account's asset and {@code toAmount} in the to-account's,
 * so cross-currency transfers carry both sides. {@code feeAmount} defaults to zero.
 */
public record TransactionCreateDto(
        String description,
        TransactionType transactionType,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal fromAmount,
        BigDecimal toAmount,
        LocalDate transactionDate,
        String sourceSystemCode,
        BigDecimal feeAmount
) {
}
