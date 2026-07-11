package org.hameed.hameedmoneycli.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    public static final String DEFAULT_TIMEZONE = "Africa/Cairo";
    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yyyy";
    public static final String DEFAULT_DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";

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
