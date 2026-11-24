package org.hameed.hameedmoneycomputer.model.dto;

import org.hameed.hameedmoneycomputer.enums.AccountType;

public record AccountCreateDto (
        String name,
        AccountType accountType,
        Long parentAccountId,
        Long assetId,
        Boolean isInternal
) {
}
