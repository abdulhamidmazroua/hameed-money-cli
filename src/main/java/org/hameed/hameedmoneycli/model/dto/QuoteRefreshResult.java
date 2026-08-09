package org.hameed.hameedmoneycli.model.dto;

import java.util.List;

public record QuoteRefreshResult(
        List<String> updated,
        List<Failure> failed
) {
    public record Failure(String baseSymbol, String quoteSymbol, String reason) {}
}
