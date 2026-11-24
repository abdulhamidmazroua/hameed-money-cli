package org.hameed.hameedmoneycomputer.model.dto;

import org.hameed.hameedmoneycomputer.enums.TransactionType;

import java.math.BigDecimal;

public record TransactionCreateDto(
        String description,
        TransactionType transactionType,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal fromAmount,
        BigDecimal toAmount,
        String transactionDate,
        String sourceSystemCode,
        BigDecimal feeAmount
) {
}
