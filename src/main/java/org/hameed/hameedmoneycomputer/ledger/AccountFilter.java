package org.hameed.hameedmoneycomputer.ledger;

/** Account search criteria; null fields are ignored. */
public record AccountFilter(
        String keyword,
        AccountType type,
        String assetSymbol
) {
    public static AccountFilter all() {
        return new AccountFilter(null, null, null);
    }
}
