package org.hameed.hameedmoneycomputer.market;

/** Asset search criteria; null fields are ignored. The keyword matches name or symbol. */
public record AssetFilter(
        String keyword,
        AssetCategory category,
        Boolean tradable
) {
}
