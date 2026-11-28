package org.hameed.hameedmoneycomputer.market;

import org.hameed.hameedmoneycomputer.shared.ValidationException;

import lombok.Getter;

@Getter
public enum AssetCategory {
    STOCK("stock"),
    ETF("etf"),
    MUTUAL_FUND("fund"),
    CASH("cash"),
    CRYPTO("crypto"),
    COMMODITY("commodity"),
    PROPERTY("property");

    private String category;

    AssetCategory(String category) {
        this.category = category;
    }

    public static AssetCategory fromString(String category) {
        for (AssetCategory assetCategory : AssetCategory.values()) {
            if (assetCategory.getCategory().equalsIgnoreCase(category)) {
                return assetCategory;
            }
        }
        throw new ValidationException("Unknown asset category '" + category + "'. Valid: "
                + java.util.Arrays.stream(values()).map(AssetCategory::getCategory).toList());
    }

    public boolean isTradable() {
        return this == STOCK || this == ETF || this == MUTUAL_FUND ||
                this == CRYPTO || this == COMMODITY;
    }

    public boolean isSecurity() {
        return this == STOCK || this == ETF || this == MUTUAL_FUND;
    }

}
