package org.hameed.hameedmoneycli.model.dto;

import java.math.BigDecimal;

public record TransactionFilter(
        String transactionType,
        Long fromAccountId,
        Long toAccountId,
        String transactionDateTimeFrom,
        String transactionDateTimeTo,
        String description,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Long involvedAccountId
) {

    public TransactionFilter(String transactionType, Long fromAccountId, Long toAccountId,
                             String transactionDateTimeFrom, String transactionDateTimeTo) {
        this(transactionType, fromAccountId, toAccountId,
                transactionDateTimeFrom, transactionDateTimeTo,
                null, null, null, null);
    }
}
