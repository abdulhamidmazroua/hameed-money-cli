package org.hameed.hameedmoneycli.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SourceFormatConfig(
        @JsonProperty("delimiter") String delimiter,
        @JsonProperty("hasHeader") Boolean hasHeader,
        @JsonProperty("skipLines") Integer skipLines,
        @JsonProperty("columns") List<ColumnMapping> columns,
        @JsonProperty("dateFormats") List<String> dateFormats,
        @JsonProperty("amountPattern") String amountPattern
) {
    public SourceFormatConfig {
        if (delimiter == null) delimiter = ",";
        if (hasHeader == null) hasHeader = true;
        if (skipLines == null) skipLines = 0;
        if (amountPattern == null) amountPattern = "signed";
    }

    public record ColumnMapping(
            @JsonProperty("index") int index,
            @JsonProperty("field") String field,
            @JsonProperty("name") String name
    ) {}

    public ColumnMapping column(String field) {
        return columns.stream()
                .filter(c -> c.field().equals(field))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No column mapping for field '" + field + "' in source config"));
    }
}
