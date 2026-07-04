package org.hameed.hameedmoneycli.model.dto;

import org.hameed.hameedmoneycli.enums.AssetCategory;

public record AssetCreateDto (
        String name,
        String symbol,
        AssetCategory category
) {
}
