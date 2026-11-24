package org.hameed.hameedmoneycomputer.model.dto;

import org.hameed.hameedmoneycomputer.enums.AssetCategory;

public record AssetCreateDto (
        String name,
        String symbol,
        AssetCategory category
) {
}
