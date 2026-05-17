package org.hameed.hameedmoneycli.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    public static final String DEFAULT_TIMEZONE = "Africa/Cairo";
    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yyyy";
    public static final String DEFAULT_DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";


    // 1- convert string date to Instant
    public static Instant parseDateStringToInstant(String dateStr) {
        return parseDateStringToInstant(dateStr, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    }

    public static Instant parseDateStringToInstant(String dateStr, DateTimeFormatter formatter) {
        LocalDate date = LocalDate.parse(dateStr, formatter);
        return date.atStartOfDay(ZoneId.of(DEFAULT_TIMEZONE)).toInstant();
    }

    // 2- convert Instant to string date
    public static String getDateStringFromInstant(Instant instant) {
        return getDateStringFromInstant(instant, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    }

    public static String getDateStringFromInstant(Instant instant, DateTimeFormatter formatter) {
        return formatter.format(instant.atZone(ZoneId.of(DEFAULT_TIMEZONE)));
    }

    // 3- convert string datetime to Instant
    public static Instant parseDateTimeStringToInstant(String dateTimeStr) {
        return parseDateTimeStringToInstant(dateTimeStr, DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT));
    }

    public static Instant parseDateTimeStringToInstant(String dateTimeStr, DateTimeFormatter formatter) {
        return formatter.parse(dateTimeStr, Instant::from);
    }

    // 4- convert Instant to string datetime
    public static String getDateTimeStringFromInstant(Instant instant) {
        return getDateTimeStringFromInstant(instant, DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT));
    }

    public static String getDateTimeStringFromInstant(Instant instant, DateTimeFormatter formatter) {
        return formatter.format(instant.atZone(ZoneId.of(DEFAULT_TIMEZONE)));
    }

}
