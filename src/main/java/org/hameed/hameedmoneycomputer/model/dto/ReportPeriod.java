package org.hameed.hameedmoneycomputer.model.dto;

import org.hameed.hameedmoneycomputer.util.DateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * An inclusive date range for reports. A null {@code start} or {@code end} means the range is open on that side.
 */
public record ReportPeriod(LocalDate start, LocalDate end, String label) {

    public static final String DEFAULT_PRESET = "this-month";

    public enum Preset {
        THIS_MONTH("this-month", "This month"),
        LAST_MONTH("last-month", "Last month"),
        THIS_YEAR("this-year", "This year"),
        LAST_YEAR("last-year", "Last year"),
        ALL("all", "All time");

        private final String token;
        private final String label;

        Preset(String token, String label) {
            this.token = token;
            this.label = label;
        }

        public String token() {
            return token;
        }

        static Preset fromToken(String token) {
            String normalized = token.trim().toLowerCase();
            for (Preset preset : values()) {
                if (preset.token.equals(normalized)) {
                    return preset;
                }
            }
            return null;
        }
    }

    public ReportPeriod {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Start date " + DateUtil.formatDate(start)
                    + " is after end date " + DateUtil.formatDate(end));
        }
    }

    public static boolean isPreset(String token) {
        return token != null && Preset.fromToken(token) != null;
    }

    public static String presetTokens() {
        return Arrays.stream(Preset.values()).map(Preset::token).collect(Collectors.joining(", "));
    }

    /**
     * Resolves the period from user input. Explicit dates win over the default preset; combining an explicit
     * preset with dates is rejected because the intent is ambiguous.
     *
     * @param preset   preset token, or null
     * @param startRaw start date (dd-MM-yyyy or yyyy-MM-dd), or null
     * @param endRaw   end date (inclusive), or null
     * @param today    the current date in the app timezone
     */
    public static ReportPeriod resolve(String preset, String startRaw, String endRaw, LocalDate today) {
        boolean hasPreset = preset != null && !preset.isBlank();
        boolean hasDates = (startRaw != null && !startRaw.isBlank()) || (endRaw != null && !endRaw.isBlank());

        if (hasPreset && hasDates) {
            throw new IllegalArgumentException("Use either a period (" + presetTokens()
                    + ") or --start-date/--end-date, not both");
        }
        if (hasDates) {
            return custom(startRaw, endRaw, today);
        }
        return fromPreset(hasPreset ? preset : DEFAULT_PRESET, today);
    }

    private static ReportPeriod fromPreset(String token, LocalDate today) {
        Preset preset = Preset.fromToken(token);
        if (preset == null) {
            throw new IllegalArgumentException("Unknown period '" + token + "'. Valid periods: " + presetTokens());
        }
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate firstOfYear = today.withDayOfYear(1);
        return switch (preset) {
            case THIS_MONTH -> new ReportPeriod(firstOfMonth, firstOfMonth.plusMonths(1).minusDays(1), preset.label);
            case LAST_MONTH -> new ReportPeriod(firstOfMonth.minusMonths(1), firstOfMonth.minusDays(1), preset.label);
            case THIS_YEAR -> new ReportPeriod(firstOfYear, firstOfYear.plusYears(1).minusDays(1), preset.label);
            case LAST_YEAR -> new ReportPeriod(firstOfYear.minusYears(1), firstOfYear.minusDays(1), preset.label);
            case ALL -> new ReportPeriod(null, null, preset.label);
        };
    }

    private static ReportPeriod custom(String startRaw, String endRaw, LocalDate today) {
        LocalDate start = startRaw == null || startRaw.isBlank() ? null : DateUtil.parseUserDate(startRaw);
        LocalDate end = endRaw == null || endRaw.isBlank() ? null : DateUtil.parseUserDate(endRaw);
        if (start != null && end == null) {
            end = today.isBefore(start) ? start : today;
        }
        return new ReportPeriod(start, end, "Custom");
    }

    /** Inclusive lower bound in epoch millis (start of day in the app timezone). */
    public long startMillis() {
        return start == null ? Long.MIN_VALUE : start.atStartOfDay(DateUtil.zone()).toInstant().toEpochMilli();
    }

    /** Exclusive upper bound in epoch millis (start of the day after {@code end} in the app timezone). */
    public long endExclusiveMillis() {
        return end == null ? Long.MAX_VALUE : end.plusDays(1).atStartOfDay(DateUtil.zone()).toInstant().toEpochMilli();
    }

    /** Average Gregorian month length in days (365.2425 / 12). */
    private static final BigDecimal AVG_DAYS_PER_MONTH = new BigDecimal("30.436875");

    /**
     * Number of months that have actually elapsed in this period, as a fraction (days / average month length).
     * The future part of the period is ignored, so "this-month" on the 15th counts about half a month.
     * An open start falls back to {@code firstActivity}; with no activity at all it returns zero.
     *
     * @param firstActivity date of the earliest transaction, or null when there are none
     * @param today         the current date in the app timezone
     */
    public BigDecimal elapsedMonths(LocalDate firstActivity, LocalDate today) {
        LocalDate from = start != null ? start : firstActivity;
        if (from == null) {
            return BigDecimal.ZERO;
        }
        LocalDate to = end == null || end.isAfter(today) ? today : end;
        if (to.isBefore(from)) {
            return BigDecimal.ZERO;
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return BigDecimal.valueOf(days).divide(AVG_DAYS_PER_MONTH, 4, RoundingMode.HALF_UP);
    }

    public String describe() {
        if (start == null && end == null) {
            return label;
        }
        String range;
        if (start == null) {
            range = "until " + DateUtil.formatDate(end);
        } else {
            range = DateUtil.formatDate(start) + " → " + DateUtil.formatDate(end);
        }
        return label + " (" + range + ")";
    }
}
