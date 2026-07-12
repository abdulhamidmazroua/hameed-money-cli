package org.hameed.hameedmoneycli.ingestion;

import java.math.BigDecimal;

public final class AmountParser {

    private AmountParser() {}

    public static BigDecimal parseSigned(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Amount is blank");
        }
        String s = raw.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        s = s.replace(",", "").trim();
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid signed amount: " + raw, e);
        }
    }

    /**
     * Two-column amount pattern: debit is negative (outflow), credit is positive (inflow).
     * Only one of the two should be non-empty in a row.
     */
    public static BigDecimal parseDebitCredit(String debitRaw, String creditRaw) {
        String debit = cleanAmount(debitRaw);
        String credit = cleanAmount(creditRaw);
        boolean hasDebit = debit != null && !debit.isEmpty();
        boolean hasCredit = credit != null && !credit.isEmpty();
        if (hasDebit && hasCredit) {
            throw new IllegalArgumentException("Both debit and credit are non-empty: debit=" + debitRaw + ", credit=" + creditRaw);
        }
        if (hasDebit) {
            return parseSigned(debit).abs().negate();
        }
        if (hasCredit) {
            return parseSigned(credit);
        }
        throw new IllegalArgumentException("Both debit and credit are empty");
    }

    private static String cleanAmount(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace(",", "").trim();
    }

    public static BigDecimal abs(BigDecimal signed) {
        return signed.abs();
    }
}
