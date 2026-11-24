package org.hameed.hameedmoneycomputer.model.dto;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;

public record IncomeReport(
        BigDecimal totalIncome,
        String currency,
        ReportPeriod period,
        List<IncomeLine> lines
) implements Report {

    /**
     * @param nativeAmount    net income in the account's own asset (Σfrom − Σto)
     * @param convertedAmount nativeAmount converted to the report currency
     */
    public record IncomeLine(String accountName, BigDecimal nativeAmount, BigDecimal convertedAmount, String assetSymbol) {}

    @Override
    public void terminalPrint(PrintWriter out) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        String sep = "──────────────────────────────────────────────────────────";

        out.println(sep);
        out.printf("  Income Report — %s%n", currency);
        out.printf("  Period: %s%n", period.describe());
        out.println(sep);

        if (lines.isEmpty()) {
            out.println("  No income recorded for this period.");
            out.println(sep);
            return;
        }

        out.println("  INCOME BY SOURCE");
        boolean anyForeign = false;
        for (IncomeLine line : lines) {
            boolean foreign = !currency.equalsIgnoreCase(line.assetSymbol());
            anyForeign |= foreign;
            String nativePart = foreign
                    ? "  (" + nf.format(line.nativeAmount()) + " " + line.assetSymbol() + ")"
                    : "";
            out.printf("    %-31s %14s %s %6s%s%n",
                    line.accountName(),
                    nf.format(line.convertedAmount()),
                    currency,
                    share(line.convertedAmount()),
                    nativePart);
        }
        out.printf("  %-33s %14s%n", "", "──────────────");
        out.printf("  %-33s %14s %s%n", "Total Income", nf.format(totalIncome), currency);
        out.println(sep);
        if (anyForeign) {
            out.println("  Foreign amounts converted at current rates.");
        }
    }

    private String share(BigDecimal amount) {
        if (totalIncome.signum() <= 0) {
            return "";
        }
        BigDecimal pct = amount.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 1, RoundingMode.HALF_UP);
        return pct.toPlainString() + "%";
    }
}
