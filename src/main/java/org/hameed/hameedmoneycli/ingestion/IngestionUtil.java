package org.hameed.hameedmoneycli.ingestion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class IngestionUtil {

    private IngestionUtil() {
    }

    public static String externalRefId(String sourceSystemCode, String dateRaw, String description, String amountRaw) {
        String normalized = String.join("|",
                sourceSystemCode,
                dateRaw != null ? dateRaw.trim() : "",
                description != null ? description.trim() : "",
                amountRaw != null ? amountRaw.trim().replace(" ", "") : "");
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
}
