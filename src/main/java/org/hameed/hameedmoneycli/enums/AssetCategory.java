package org.hameed.hameedmoneycli.enums;

import lombok.Getter;

@Getter
public enum AssetCategory {
    STOCK("stock"),
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
        throw new IllegalArgumentException("No enum constant for category: " + category);
    }

    // TODO: remove this later and define more business rules to determine if an asset is tradable or not (maybe one category can be tradable or not based on other factors)
    public boolean isTradable() {
        return
                (this == STOCK ||
                this == CRYPTO ||
                this == COMMODITY);
    }

}
