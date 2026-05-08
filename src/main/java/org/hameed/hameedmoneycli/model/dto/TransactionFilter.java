package org.hameed.hameedmoneycli.model.dto;

public record TransactionFilter(
            String transactionType,
            Long fromAccountId,
            Long toAccountId,
            String transactionDateTimeFrom,
            String transactionDateTimeTo
) {
}
