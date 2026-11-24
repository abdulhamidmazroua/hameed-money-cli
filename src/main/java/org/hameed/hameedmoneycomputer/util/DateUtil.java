package org.hameed.hameedmoneycomputer.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DateUtil {

    public static final String DEFAULT_TIMEZONE = "Africa/Cairo";
    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yyyy";
    public static final String DEFAULT_DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";

    private static final List<DateTimeFormatter> USER_DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    public static ZoneId zone() {
        return ZoneId.of(DEFAULT_TIMEZONE);
    }

    /**
     * Parses a date typed by the user. Accepts the app default {@code dd-MM-yyyy} and ISO {@code yyyy-MM-dd}.
     */
    public static LocalDate parseUserDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Date must not be empty. Use dd-MM-yyyy (e.g. 01-10-2026)");
        }
        String value = raw.trim();
        for (DateTimeFormatter formatter : USER_DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        throw new IllegalArgumentException("Invalid date '" + value + "'. Use dd-MM-yyyy (e.g. 01-10-2026)");
    }

    public static String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    }

    public static Long parseDateStringToMillis(String dateStr) {
        return parseDateStringToMillis(dateStr, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    }

    public static Long parseDateStringToMillis(String dateStr, DateTimeFormatter formatter) {
        LocalDate date = LocalDate.parse(dateStr, formatter);
        return date.atStartOfDay(ZoneId.of(DEFAULT_TIMEZONE)).toInstant().toEpochMilli();
    }

    public static String getDateStringFromMillis(Long millis) {
        return getDateStringFromMillis(millis, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    }

    public static String getDateStringFromMillis(Long millis, DateTimeFormatter formatter) {
        if (millis == null) return null;
        return formatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.of(DEFAULT_TIMEZONE)));
    }

    public static Long parseDateTimeStringToMillis(String dateTimeStr) {
        return parseDateTimeStringToMillis(dateTimeStr, DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT));
    }

    public static Long parseDateTimeStringToMillis(String dateTimeStr, DateTimeFormatter formatter) {
        return formatter.parse(dateTimeStr, Instant::from).toEpochMilli();
    }

    public static String getDateTimeStringFromMillis(Long millis) {
        return getDateTimeStringFromMillis(millis, DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT));
    }

    public static String getDateTimeStringFromMillis(Long millis, DateTimeFormatter formatter) {
        if (millis == null) return null;
        return formatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.of(DEFAULT_TIMEZONE)));
    }
}
