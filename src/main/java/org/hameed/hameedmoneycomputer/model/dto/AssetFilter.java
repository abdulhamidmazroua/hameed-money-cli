package org.hameed.hameedmoneycomputer.model.dto;

public record AssetFilter(
        String keyword,
        String category,
        Boolean tradable
) {
}
