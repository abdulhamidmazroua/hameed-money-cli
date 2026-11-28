package org.hameed.hameedmoneycomputer.reporting;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cash flow for a period: Net = Income − Expenses. Burn rate is expenses per elapsed month and runway is
 * liquid cash (current CASH-category asset balances) divided by that burn.
 *
 * @param savingsRate    net / income as a percentage, or null when income ≤ 0
 * @param avgMonthlyBurn expenses / elapsed months, or null when no time has elapsed
 * @param runwayMonths   liquidCash / avgMonthlyBurn, or null when burn ≤ 0 (infinite runway)
 */
public record CashflowReport(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netCashFlow,
        BigDecimal savingsRate,
        BigDecimal elapsedMonths,
        BigDecimal avgMonthlyBurn,
        BigDecimal liquidCash,
        BigDecimal runwayMonths,
        String currency,
        ReportPeriod period,
        List<FlowLine> incomeLines,
        List<FlowLine> expenseLines
) {

    /**
     * @param nativeAmount    net flow in the account's own asset
     * @param convertedAmount nativeAmount converted to the report currency
     */
    public record FlowLine(String accountName, BigDecimal nativeAmount, BigDecimal convertedAmount, String assetSymbol) {}
}
