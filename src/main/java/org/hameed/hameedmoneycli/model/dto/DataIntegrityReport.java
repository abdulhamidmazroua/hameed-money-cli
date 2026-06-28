package org.hameed.hameedmoneycli.model.dto;

import java.math.BigDecimal;
import java.util.List;

public record DataIntegrityReport(
        Section openingBalances,
        Section increaseAdjustments,
        Section decreaseAdjustments,
        BigDecimal systemHealth
) implements Report {

    public record Section(int count, BigDecimal total, List<AssetLine> breakdown) {}
    public record AssetLine(String symbol, BigDecimal amount) {}
}
