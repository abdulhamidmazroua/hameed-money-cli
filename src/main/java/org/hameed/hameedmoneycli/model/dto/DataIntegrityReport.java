package org.hameed.hameedmoneycli.model.dto;

import java.io.PrintWriter;
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

    @Override
    public void terminalPrint(PrintWriter out) {
        out.println("--- Data Integrity Report ---");

        printSection(out, "Opening Balances", openingBalances);
        printSection(out, "Balance Increase Adjustments", increaseAdjustments);
        printSection(out, "Balance Decrease Adjustments", decreaseAdjustments);

        out.println();
        out.printf("System Health: %.1f%%%n",
                systemHealth.multiply(BigDecimal.valueOf(100)));
    }

    private void printSection(PrintWriter out, String title, Section section) {
        out.println("  " + title + ":");
        out.println("    Total Count: " + section.count());
        out.println("    Total " + title + ":");
        for (AssetLine line : section.breakdown()) {
            out.printf("      %s %s%n", line.amount().stripTrailingZeros().toPlainString(), line.symbol());
        }
        out.println();
    }
}
