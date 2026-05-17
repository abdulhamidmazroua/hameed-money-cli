package org.hameed.hameedmoneycli.model.dto;

import org.hameed.hameedmoneycli.enums.SourceSystemCode;
import org.hameed.hameedmoneycli.enums.TransactionType;

import java.math.BigDecimal;

public record TransactionCreateDto(
        String description,
        TransactionType transactionType,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal fromAmount,
        BigDecimal toAmount,
        String transactionDate,
        SourceSystemCode sourceSystemCode,
        BigDecimal feeAmount
) {
}
