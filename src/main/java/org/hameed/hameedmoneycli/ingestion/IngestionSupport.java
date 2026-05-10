package org.hameed.hameedmoneycli.ingestion;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Shared helpers for CSV ingestion: idempotency keys and amount parsing.
 */
public final class IngestionSupport {

    private IngestionSupport() {
    }

    /**
     * Stable idempotency key for the same logical bank row across re-imports.
     */
    public static String externalRefId(String sourceSystemCode, String dateRaw, String description, String amountRaw) {
        String normalized = String.join("|",
                sourceSystemCode,
                dateRaw.trim(),
                description.trim(),
                amountRaw.trim().replace(" ", ""));
        return sha256Hex(normalized);
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Parses HSBC-style amounts: optional thousands separators, optional quotes, leading sign.
     */
    public static BigDecimal parseSignedAmount(String raw) {
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
            throw new IllegalArgumentException("Invalid amount: " + raw, e);
        }
    }

    public static BigDecimal absAmount(BigDecimal signed) {
        return signed.abs();
    }
}
