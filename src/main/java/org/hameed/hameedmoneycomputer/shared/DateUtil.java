package org.hameed.hameedmoneycomputer.shared;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/**
 * Date parsing and formatting in the app timezone. The zone is the system timezone unless config.json sets
 * {@code "timezone"} (applied at startup via {@link #useZone(ZoneId)}).
 */
public final class DateUtil {

    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yyyy";
    public static final String DEFAULT_DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT);
    private static final List<DateTimeFormatter> USER_DATE_FORMATS = List.of(DATE, DateTimeFormatter.ISO_LOCAL_DATE);

    private static volatile ZoneId zone = ZoneId.systemDefault();

    private DateUtil() {
    }

    public static ZoneId zone() {
        return zone;
    }

    public static void useZone(ZoneId newZone) {
        zone = Objects.requireNonNull(newZone);
    }

    public static LocalDate today() {
        return LocalDate.now(zone);
    }

    /**
     * Parses a date typed by the user. Accepts the app default {@code dd-MM-yyyy} and ISO {@code yyyy-MM-dd}.
     */
    public static LocalDate parseUserDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Date must not be empty. Use dd-MM-yyyy (e.g. 01-10-2026)");
        }
        String value = raw.trim();
        for (DateTimeFormatter formatter : USER_DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        throw new ValidationException("Invalid date '" + value + "'. Use dd-MM-yyyy (e.g. 01-10-2026)");
    }

    /** Start of {@code date} in the app timezone, as epoch millis. */
    public static long startOfDayMillis(LocalDate date) {
        return date.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    public static LocalDate toLocalDate(Instant instant) {
        return instant == null ? null : instant.atZone(zone).toLocalDate();
    }

    public static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE);
    }

    public static String formatDateTime(Instant instant) {
        return instant == null ? null : DATE_TIME.format(instant.atZone(zone));
    }

    /** Parses {@code dateStr} with {@code formatter} to the start of that day in the app timezone. */
    public static Long parseDateStringToMillis(String dateStr, DateTimeFormatter formatter) {
        return startOfDayMillis(LocalDate.parse(dateStr, formatter));
    }

    public static String getDateStringFromMillis(Long millis, DateTimeFormatter formatter) {
        if (millis == null) return null;
        return formatter.format(Instant.ofEpochMilli(millis).atZone(zone));
    }
}
