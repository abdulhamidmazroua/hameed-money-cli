package org.hameed.hameedmoneycomputer.model.dto;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
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
) implements Report {

    /**
     * @param nativeAmount    net flow in the account's own asset
     * @param convertedAmount nativeAmount converted to the report currency
     */
    public record FlowLine(String accountName, BigDecimal nativeAmount, BigDecimal convertedAmount, String assetSymbol) {}

    private static final String SEP = "──────────────────────────────────────────────────────────";

    @Override
    public void terminalPrint(PrintWriter out) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        out.println(SEP);
        out.printf("  Cash Flow Report — %s%n", currency);
        out.printf("  Period: %s%n", period.describe());
        out.println(SEP);

        boolean anyForeign = printSection(out, nf, "INFLOWS (Income)", incomeLines, totalIncome, "Total Income");
        out.println();
        anyForeign |= printSection(out, nf, "OUTFLOWS (Expenses)", expenseLines, totalExpenses, "Total Expenses");
        out.println(SEP);

        out.println("  SUMMARY");
        out.printf("    %-31s %14s %s%n", "Net Cash Flow", signed(nf, netCashFlow), currency);
        out.printf("    %-31s %14s%n", "Savings Rate",
                savingsRate == null ? "n/a" : savingsRate.setScale(1, RoundingMode.HALF_UP).toPlainString() + "%");
        out.printf("    %-31s %14s%n", "Elapsed Months", elapsedMonths.setScale(2, RoundingMode.HALF_UP).toPlainString());
        out.printf("    %-31s %14s %s%n", "Avg Monthly Burn",
                avgMonthlyBurn == null ? "n/a" : nf.format(avgMonthlyBurn), currency);
        out.printf("    %-31s %14s %s%n", "Liquid Cash (now)", nf.format(liquidCash), currency);
        out.printf("    %-31s %14s%n", "Runway",
                runwayMonths == null ? "∞" : runwayMonths.setScale(1, RoundingMode.HALF_UP).toPlainString() + " months");
        out.println(SEP);
        if (anyForeign) {
            out.println("  Foreign amounts converted at current rates.");
        }
        out.println("  Transfers and system adjustments are excluded.");
    }

    private boolean printSection(PrintWriter out, NumberFormat nf, String title, List<FlowLine> lines,
                                 BigDecimal total, String totalLabel) {
        out.println("  " + title);
        if (lines.isEmpty()) {
            out.println("    (none)");
            return false;
        }
        boolean anyForeign = false;
        for (FlowLine line : lines) {
            boolean foreign = !currency.equalsIgnoreCase(line.assetSymbol());
            anyForeign |= foreign;
            String nativePart = foreign
                    ? "  (" + nf.format(line.nativeAmount()) + " " + line.assetSymbol() + ")"
                    : "";
            out.printf("    %-31s %14s %s %6s%s%n",
                    line.accountName(), nf.format(line.convertedAmount()), currency,
                    share(line.convertedAmount(), total), nativePart);
        }
        out.printf("  %-33s %14s%n", "", "──────────────");
        out.printf("  %-33s %14s %s%n", totalLabel, nf.format(total), currency);
        return anyForeign;
    }

    private static String signed(NumberFormat nf, BigDecimal value) {
        return (value.signum() > 0 ? "+" : "") + nf.format(value);
    }

    private static String share(BigDecimal amount, BigDecimal total) {
        if (total.signum() <= 0) {
            return "";
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
