package org.hameed.hameedmoneycli.model.dto;

import org.hameed.hameedmoneycli.enums.SourceSystem;
import org.hameed.hameedmoneycli.enums.TransactionType;

import java.math.BigDecimal;

public record TransactionCreateDto(
        String description,
        TransactionType transactionType,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal fromAmount,
        BigDecimal toAmount,
        String transactionDateTime,
        SourceSystem sourceSystem,
        BigDecimal feeAmount
) {
}
