package org.hameed.hameedmoneycomputer.reporting;

import java.math.BigDecimal;
import java.util.List;

/**
 * How much of the ledger comes from system adjustments rather than real transactions.
 * {@code systemHealth} = 1 − (total adjustments / total volume), as a fraction.
 */
public record DataIntegrityReport(
        Section openingBalances,
        Section increaseAdjustments,
        Section decreaseAdjustments,
        BigDecimal systemHealth
) {
    public record Section(int count, BigDecimal total, List<AssetLine> breakdown) {}
    public record AssetLine(String symbol, BigDecimal amount) {}
}
