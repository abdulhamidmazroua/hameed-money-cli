package org.hameed.hameedmoneycli.ingestion;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hameed.hameedmoneycli.model.entity.SourceFormatConfig;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GenericParser {

    private static final String[] FALLBACK_DATE_FORMATS = {
            "dd/MM/yyyy", "d MMM yyyy", "yyyy-MM-dd", "MMM dd yyyy",
            "dd-MMM-yyyy", "MM/dd/yyyy", "yyyy/MM/dd", "dd.MM.yyyy",
            "d/M/yyyy", "M/d/yyyy", "d-MMM-yyyy", "dd MMM yyyy"
    };

    private GenericParser() {}

    public static List<ParsedRow> parse(String filePath, SourceFormatConfig config) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(config.delimiter())
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        List<ParsedRow> rows = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(Path.of(filePath));
             CSVParser parser = format.parse(reader)) {

            int rowIndex = 0;
            for (CSVRecord record : parser) {
                int currentIndex = rowIndex++;

                if (config.hasHeader() && currentIndex == 0) {
                    continue;
                }
                if (currentIndex < config.skipLines()) {
                    continue;
                }

                ParsedRow row = parseRow(record, config, currentIndex);
                rows.add(row);
            }
        }
        return rows;
    }

    public static ParsedRow parseRow(CSVRecord record, SourceFormatConfig config, int rowIndex) {
        String rawDate = getColumn(record, config, "date");
        String rawDescription = getColumn(record, config, "description");
        String rawAmount = getColumn(record, config, "amount", null);
        String rawDebit = getColumn(record, config, "debit", null);
        String rawCredit = getColumn(record, config, "credit", null);

        if ("debit_credit".equals(config.amountPattern())) {
            rawAmount = (rawDebit != null ? rawDebit : "") + "|" + (rawCredit != null ? rawCredit : "");
        }

        if (rawDate == null || rawDate.isBlank()) {
            return ParsedRow.error(rowIndex, rawDate, rawDescription, rawAmount, "Date column is empty");
        }
        if (rawDescription == null || rawDescription.isBlank()) {
            return ParsedRow.error(rowIndex, rawDate, rawDescription, rawAmount, "Description column is empty");
        }

        Long parsedDate = null;
        String dateError = null;
        for (String dateFormat : config.dateFormats()) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
                parsedDate = org.hameed.hameedmoneycli.util.DateUtil.parseDateStringToMillis(rawDate.trim(), fmt);
                break;
            } catch (DateTimeParseException ignored) {
            }
        }
        if (parsedDate == null) {
            for (String dateFormat : FALLBACK_DATE_FORMATS) {
                try {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH);
                    parsedDate = org.hameed.hameedmoneycli.util.DateUtil.parseDateStringToMillis(rawDate.trim(), fmt);
                    break;
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        if (parsedDate == null) {
            dateError = "Could not parse date '" + rawDate + "' with configured formats: " + config.dateFormats() + " or fallbacks: " + String.join(", ", FALLBACK_DATE_FORMATS);
        }

        BigDecimal parsedAmount = null;
        String amountError = null;
        try {
            parsedAmount = switch (config.amountPattern()) {
                case "debit_credit" -> AmountParser.parseDebitCredit(rawDebit, rawCredit);
                default -> AmountParser.parseSigned(rawAmount);
            };
        } catch (IllegalArgumentException e) {
            amountError = e.getMessage();
        }

        return new ParsedRow(rowIndex, rawDate, rawDescription, rawAmount, parsedDate, parsedAmount, dateError, amountError);
    }

    private static String getColumn(CSVRecord record, SourceFormatConfig config, String field) {
        return getColumn(record, config, field, "");
    }

    private static String getColumn(CSVRecord record, SourceFormatConfig config, String field, String fallback) {
        try {
            SourceFormatConfig.ColumnMapping mapping = config.column(field);
            if (mapping.index() < record.size()) {
                return record.get(mapping.index());
            }
            return fallback;
        } catch (IllegalArgumentException e) {
            if (field.equals("debit") || field.equals("credit") || field.equals("amount")) {
                return fallback;
            }
            throw e;
        }
    }

    public record ParsedRow(
            int rowIndex,
            String rawDate,
            String rawDescription,
            String rawAmount,
            Long parsedDate,
            BigDecimal parsedAmount,
            String dateError,
            String amountError
    ) {
        public boolean hasError() {
            return dateError != null || amountError != null;
        }

        public String errorMessage() {
            StringBuilder sb = new StringBuilder();
            if (dateError != null) sb.append(dateError);
            if (amountError != null) {
                if (!sb.isEmpty()) sb.append("; ");
                sb.append(amountError);
            }
            return sb.toString();
        }

        public static ParsedRow error(int rowIndex, String rawDate, String rawDesc, String rawAmount, String error) {
            return new ParsedRow(rowIndex, rawDate, rawDesc, rawAmount, null, null, null, error);
        }
    }
}
