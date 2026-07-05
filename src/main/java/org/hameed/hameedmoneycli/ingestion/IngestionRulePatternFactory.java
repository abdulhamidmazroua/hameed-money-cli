package org.hameed.hameedmoneycli.ingestion;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

/**
 * Builds Java regex strings for {@code ingestion_rule.match_pattern} from CLI input.
 */
@UtilityClass
public final class IngestionRulePatternFactory {

    /**
     * @param userInput raw line from the user; empty uses a short snippet of {@code fullDescription}
     * @param fullDescription original bank description (fallback when input is blank)
     */
    public static String toMatchPattern(String userInput, String fullDescription) {
        String trimmed = userInput == null ? "" : userInput.trim();
        if (trimmed.isEmpty()) {
            return "(?i).*" + Pattern.quote(defaultSnippet(fullDescription)) + ".*";
        }
        if (looksLikeRawRegex(trimmed)) {
            // Validate early so the user gets a clear error
            Pattern.compile(trimmed, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            return trimmed;
        }
        return "(?i).*" + Pattern.quote(trimmed) + ".*";
    }

    private static boolean looksLikeRawRegex(String s) {
        return s.startsWith("(?")
                || s.startsWith("^")
                || s.startsWith("[")
                || s.contains("|");
    }

    private static String defaultSnippet(String fullDescription) {
        if (fullDescription == null || fullDescription.isBlank()) {
            return "";
        }
        String d = fullDescription.trim();
        String stripped = d.replaceFirst("(?i)^TRANSFER", "")
                .replaceFirst("(?i)^CARD TRANSACTION\\s*\\S*\\s*", "")
                .trim();
        if (stripped.length() < 8) {
            stripped = d;
        }
        int n = Math.min(72, stripped.length());
        return stripped.substring(0, n);
    }

    /** Short text pre-filled in the CLI when teaching a new rule. */
    public static String suggestDefaultKeywordForPrompt(String fullDescription) {
        return defaultSnippet(fullDescription);
    }
}
