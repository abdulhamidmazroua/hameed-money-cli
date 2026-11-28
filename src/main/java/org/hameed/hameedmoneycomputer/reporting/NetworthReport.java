package org.hameed.hameedmoneycomputer.reporting;

import java.math.BigDecimal;
import java.util.List;

/** Balance sheet: every internal leaf account converted to {@code currency}. Net worth = assets − liabilities. */
public record NetworthReport(
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal netWorth,
        String currency,
        List<NetworthLine> assetLines,
        List<NetworthLine> liabilityLines
) {
    public record NetworthLine(String accountName, BigDecimal nativeBalance, BigDecimal convertedBalance, String assetSymbol) {}
}
