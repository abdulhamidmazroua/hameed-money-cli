package org.hameed.hameedmoneycli.model.dto;

public record TransactionDto(
        String description,
        String type,
        String transactionDateTime,
        String fromAccountName,
        String forAssetCode,
        String fromAmount,
        String toAccountName,
        String toAssetCode,
        String toAmount,
        String feeAmount,
        String sourceSystemName,
        String createdAt
) {
}
