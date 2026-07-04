package org.hameed.hameedmoneycli.model.dto;

public record AccountFilter(
        String keyword,
        String masterType,
        String assetSymbol
) {
}
