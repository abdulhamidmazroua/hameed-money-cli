package org.hameed.hameedmoneycomputer.cli;

import org.hameed.hameedmoneycomputer.ledger.AccountView;
import org.hameed.hameedmoneycomputer.reporting.AccountAudit;
import org.hameed.hameedmoneycomputer.reporting.CashflowReport;
import org.hameed.hameedmoneycomputer.reporting.DataIntegrityReport;
import org.hameed.hameedmoneycomputer.reporting.IncomeReport;
import org.hameed.hameedmoneycomputer.reporting.LedgerAudit;
import org.hameed.hameedmoneycomputer.reporting.NetworthReport;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Terminal rendering of reports and audits. */
final class ReportPrinter {

    private static final String SEP = "──────────────────────────────────────────────────────────";
    private static final String NW_SEP = "──────────────────────────────────────────────";

    private ReportPrinter() {
    }

    static void print(PrintWriter out, NetworthReport r) {
        NumberFormat nf = twoDecimals();
        out.println(NW_SEP);
        out.printf("  Net Worth Report — %s%n", r.currency());
        out.println(NW_SEP);

        printNetworthLines(out, "ASSETS", r.assetLines(), r.currency(), nf);
        if (!r.assetLines().isEmpty()) {
            out.printf("  %-33s %11s %s%n", "", "───────────", "");
        }
        out.printf("  %-33s %11.2f %s%n", "Total Assets", r.totalAssets(), r.currency());
        out.println();

        printNetworthLines(out, "LIABILITIES", r.liabilityLines(), r.currency(), nf);
        if (!r.liabilityLines().isEmpty()) {
            out.printf("  %-33s %11s %s%n", "", "───────────", "");
        }
        out.printf("  %-33s %11.2f %s%n", "Total Liabilities", r.totalLiabilities(), r.currency());

        out.println(NW_SEP);
        out.printf("  %-33s %11.2f %s%n", "NET WORTH", r.netWorth(), r.currency());
        out.println(NW_SEP);
    }

    static void print(PrintWriter out, IncomeReport r) {
        NumberFormat nf = twoDecimals();
        out.println(SEP);
        out.printf("  Income Report — %s%n", r.currency());
        out.printf("  Period: %s%n", r.period().describe());
        out.println(SEP);

        if (r.lines().isEmpty()) {
            out.println("  No income recorded for this period.");
            out.println(SEP);
            return;
        }

        out.println("  INCOME BY SOURCE");
        boolean anyForeign = false;
        for (IncomeReport.IncomeLine line : r.lines()) {
            anyForeign |= printFlowLine(out, nf, r.currency(), line.accountName(), line.nativeAmount(),
                    line.convertedAmount(), line.assetSymbol(), r.totalIncome());
        }
        out.printf("  %-33s %14s%n", "", "──────────────");
        out.printf("  %-33s %14s %s%n", "Total Income", nf.format(r.totalIncome()), r.currency());
        out.println(SEP);
        if (anyForeign) {
            out.println("  Foreign amounts converted at current rates.");
        }
    }

    static void print(PrintWriter out, CashflowReport r) {
        NumberFormat nf = twoDecimals();
        String currency = r.currency();
        out.println(SEP);
        out.printf("  Cash Flow Report — %s%n", currency);
        out.printf("  Period: %s%n", r.period().describe());
        out.println(SEP);

        boolean anyForeign = printFlowSection(out, nf, currency, "INFLOWS (Income)", r.incomeLines(), r.totalIncome(), "Total Income");
        out.println();
        anyForeign |= printFlowSection(out, nf, currency, "OUTFLOWS (Expenses)", r.expenseLines(), r.totalExpenses(), "Total Expenses");
        out.println(SEP);

        out.println("  SUMMARY");
        out.printf("    %-31s %14s %s%n", "Net Cash Flow", signed(nf, r.netCashFlow()), currency);
        out.printf("    %-31s %14s%n", "Savings Rate",
                r.savingsRate() == null ? "n/a" : r.savingsRate().setScale(1, RoundingMode.HALF_UP).toPlainString() + "%");
        out.printf("    %-31s %14s%n", "Elapsed Months", r.elapsedMonths().setScale(2, RoundingMode.HALF_UP).toPlainString());
        out.printf("    %-31s %14s %s%n", "Avg Monthly Burn",
                r.avgMonthlyBurn() == null ? "n/a" : nf.format(r.avgMonthlyBurn()), currency);
        out.printf("    %-31s %14s %s%n", "Liquid Cash (now)", nf.format(r.liquidCash()), currency);
        out.printf("    %-31s %14s%n", "Runway",
                r.runwayMonths() == null ? "∞" : r.runwayMonths().setScale(1, RoundingMode.HALF_UP).toPlainString() + " months");
        out.println(SEP);
        if (anyForeign) {
            out.println("  Foreign amounts converted at current rates.");
        }
        out.println("  Transfers and system adjustments are excluded.");
    }

    static void print(PrintWriter out, DataIntegrityReport r) {
        out.println("--- Data Integrity Report ---");

        printIntegritySection(out, "Opening Balances", r.openingBalances());
        printIntegritySection(out, "Balance Increase Adjustments", r.increaseAdjustments());
        printIntegritySection(out, "Balance Decrease Adjustments", r.decreaseAdjustments());

        out.println();
        out.printf("System Health: %.1f%%%n", r.systemHealth().multiply(BigDecimal.valueOf(100)));
    }

    static void print(PrintWriter out, AccountAudit audit) {
        AccountView account = audit.account();
        String assetLabel = account.folder() ? "(none)" : account.assetName() + " (" + account.assetSymbol() + ")";
        String dateRange = audit.firstTransaction() == null
                ? "N/A"
                : DateTimeFormatter.ISO_LOCAL_DATE.format(audit.firstTransaction()) + " to "
                  + DateTimeFormatter.ISO_LOCAL_DATE.format(audit.lastTransaction());

        out.println("--- AUDIT ACCOUNT ---");
        out.printf("Account:   %s (ID: %d)%n", account.name(), account.id());
        out.printf("Asset:     %s%n", assetLabel);
        out.printf("Type:      %s | Internal: %s%n", account.type(), account.internal());
        out.printf("Balance:   %s%n", audit.balance().toPlainString());
        out.printf("Transactions: %d (%s)%n", audit.transactionCount(), dateRange);
        out.println();
    }

    static void print(PrintWriter out, LedgerAudit audit) {
        out.println("--- AUDIT TRAIL ---");
        out.printf("Leaf accounts: %d%n", audit.leafAccounts());
        out.printf("Total transactions: %d%n", audit.totalTransactions());
        out.println();
        if (audit.anomalyCount() == 0) {
            out.println("All accounts verified ✓");
        } else {
            out.println("Anomalies found: " + audit.anomalyCount());
            for (LedgerAudit.NegativeBalance negative : audit.negativeBalances()) {
                out.printf("  - NEGATIVE BALANCE: %s (ID: %d) = %s%n",
                        negative.accountName(), negative.accountId(), negative.balance().toPlainString());
            }
            if (audit.orphanedTotal() > 0) {
                out.printf("  - ORPHANED TRANSACTIONS: %d (from: %d, to: %d)%n",
                        audit.orphanedTotal(), audit.orphanedFrom(), audit.orphanedTo());
            }
        }
        out.println();
    }

    private static void printNetworthLines(PrintWriter out, String title, List<NetworthReport.NetworthLine> lines,
                                           String currency, NumberFormat nf) {
        out.printf("  %s%n", title);
        for (NetworthReport.NetworthLine line : lines) {
            out.printf("    %-31s %11s %s%n", line.accountName(), nf.format(line.convertedBalance()), currency);
        }
    }

    /** @return true when any line is in a currency other than the report currency */
    private static boolean printFlowSection(PrintWriter out, NumberFormat nf, String currency, String title,
                                            List<CashflowReport.FlowLine> lines, BigDecimal total, String totalLabel) {
        out.println("  " + title);
        if (lines.isEmpty()) {
            out.println("    (none)");
            return false;
        }
        boolean anyForeign = false;
        for (CashflowReport.FlowLine line : lines) {
            anyForeign |= printFlowLine(out, nf, currency, line.accountName(), line.nativeAmount(),
                    line.convertedAmount(), line.assetSymbol(), total);
        }
        out.printf("  %-33s %14s%n", "", "──────────────");
        out.printf("  %-33s %14s %s%n", totalLabel, nf.format(total), currency);
        return anyForeign;
    }

    /** @return true when the line is in a currency other than the report currency */
    private static boolean printFlowLine(PrintWriter out, NumberFormat nf, String currency, String accountName,
                                         BigDecimal nativeAmount, BigDecimal convertedAmount, String assetSymbol,
                                         BigDecimal total) {
        boolean foreign = !currency.equalsIgnoreCase(assetSymbol);
        String nativePart = foreign ? "  (" + nf.format(nativeAmount) + " " + assetSymbol + ")" : "";
        out.printf("    %-31s %14s %s %6s%s%n",
                accountName, nf.format(convertedAmount), currency, share(convertedAmount, total), nativePart);
        return foreign;
    }

    private static void printIntegritySection(PrintWriter out, String title, DataIntegrityReport.Section section) {
        out.println("  " + title + ":");
        out.println("    Total Count: " + section.count());
        out.println("    Total " + title + ":");
        for (DataIntegrityReport.AssetLine line : section.breakdown()) {
            out.printf("      %s %s%n", line.amount().stripTrailingZeros().toPlainString(), line.symbol());
        }
        out.println();
    }

    private static NumberFormat twoDecimals() {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf;
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
