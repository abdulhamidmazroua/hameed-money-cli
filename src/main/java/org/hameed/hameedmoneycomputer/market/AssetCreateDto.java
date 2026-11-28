package org.hameed.hameedmoneycomputer.market;


public record AssetCreateDto (
        String name,
        String symbol,
        AssetCategory category
) {
}
