package org.hameed.hameedmoneycli.proxy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EodhdSymbolDto(
        @JsonProperty("Code") String code,
        @JsonProperty("Name") String name,
        @JsonProperty("Country") String country,
        @JsonProperty("Exchange") String exchange,
        @JsonProperty("Currency") String currency,
        @JsonProperty("Type") String type,
        @JsonProperty("Isin") String isin
) {}
