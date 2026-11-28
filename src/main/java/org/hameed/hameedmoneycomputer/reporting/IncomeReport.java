package org.hameed.hameedmoneycomputer.reporting;

import java.math.BigDecimal;
import java.util.List;

/** Net income per INCOME account in a period, converted to {@code currency}. */
public record IncomeReport(
        BigDecimal totalIncome,
        String currency,
        ReportPeriod period,
        List<IncomeLine> lines
) {
    /**
     * @param nativeAmount    net income in the account's own asset (Σfrom − Σto)
     * @param convertedAmount nativeAmount converted to the report currency
     */
    public record IncomeLine(String accountName, BigDecimal nativeAmount, BigDecimal convertedAmount, String assetSymbol) {}
}
