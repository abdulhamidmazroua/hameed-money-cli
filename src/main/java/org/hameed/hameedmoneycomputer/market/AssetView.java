package org.hameed.hameedmoneycomputer.market;

/** An asset as seen by clients. */
public record AssetView(long id, String name, String symbol, AssetCategory category, boolean tradable) {

    public static AssetView from(Asset asset) {
        return new AssetView(asset.getId(), asset.getName(), asset.getSymbol(), asset.getCategory(),
                Boolean.TRUE.equals(asset.getIsTradable()));
    }
}
