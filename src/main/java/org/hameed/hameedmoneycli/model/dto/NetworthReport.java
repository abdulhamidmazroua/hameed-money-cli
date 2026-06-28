package org.hameed.hameedmoneycli.model.dto;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;

public record NetworthReport(
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal netWorth,
        String currency,
        List<NetworthLine> assetLines,
        List<NetworthLine> liabilityLines
) implements Report {

    public record NetworthLine(String accountName, BigDecimal nativeBalance, BigDecimal convertedBalance, String assetSymbol) {}

    @Override
    public void terminalPrint(PrintWriter out) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        String sep = "──────────────────────────────────────────────";

        out.println(sep);
        out.printf("  Net Worth Report — %s%n", currency);
        out.println(sep);

        printLines(out, "ASSETS", assetLines, nf);
        if (!assetLines.isEmpty()) {
            out.printf("  %-33s %11s %s%n", "", "───────────", "");
        }
        out.printf("  %-33s %11.2f %s%n", "Total Assets", totalAssets, currency);
        out.println();

        printLines(out, "LIABILITIES", liabilityLines, nf);
        if (!liabilityLines.isEmpty()) {
            out.printf("  %-33s %11s %s%n", "", "───────────", "");
        }
        out.printf("  %-33s %11.2f %s%n", "Total Liabilities", totalLiabilities, currency);

        out.println(sep);
        out.printf("  %-33s %11.2f %s%n", "NET WORTH", netWorth, currency);
        out.println(sep);
    }

    private void printLines(PrintWriter out, String title, List<NetworthLine> lines, NumberFormat nf) {
        out.printf("  %s%n", title);
        for (NetworthLine line : lines) {
            out.printf("    %-31s %11s %s%n",
                    line.accountName(),
                    nf.format(line.convertedBalance()),
                    currency);
        }
    }
}
