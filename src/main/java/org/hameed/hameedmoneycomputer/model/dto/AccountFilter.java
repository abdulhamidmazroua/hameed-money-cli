package org.hameed.hameedmoneycomputer.model.dto;

public record AccountFilter(
        String keyword,
        String masterType,
        String assetSymbol
) {
}
