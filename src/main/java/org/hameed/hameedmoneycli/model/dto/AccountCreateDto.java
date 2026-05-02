package org.hameed.hameedmoneycli.model.dto;

import org.hameed.hameedmoneycli.enums.AccountType;

public record AccountCreateDto (
        String name,
        AccountType accountType,
        Long parentAccountId,
        Long assetId,
        Boolean isInternal
) {
}
