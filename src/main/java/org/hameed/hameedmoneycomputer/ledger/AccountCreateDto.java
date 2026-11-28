package org.hameed.hameedmoneycomputer.ledger;


public record AccountCreateDto (
        String name,
        AccountType accountType,
        Long parentAccountId,
        Long assetId,
        Boolean isInternal
) {
}
