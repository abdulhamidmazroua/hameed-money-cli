package org.hameed.hameedmoneycli.model.dto;

public record AssetFilter(
        String keyword,
        String category,
        Boolean tradable
) {
}
