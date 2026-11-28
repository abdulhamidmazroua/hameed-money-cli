package org.hameed.hameedmoneycomputer.ledger;

import org.hameed.hameedmoneycomputer.market.Asset;

/**
 * An account as seen by clients. Folders ({@code assetSymbol == null}) only organize the tree; leaf accounts hold
 * one asset and post transactions in it.
 */
public record AccountView(
        long id,
        String name,
        AccountType type,
        Long parentId,
        String assetSymbol,
        String assetName,
        boolean internal
) {
    public boolean folder() {
        return assetSymbol == null;
    }

    public static AccountView from(Account account) {
        Asset asset = account.getAsset();
        return new AccountView(
                account.getId(),
                account.getName(),
                account.getMasterType(),
                account.getParent() == null ? null : account.getParent().getId(),
                asset == null ? null : asset.getSymbol(),
                asset == null ? null : asset.getName(),
                Boolean.TRUE.equals(account.getIsInternal()));
    }
}
