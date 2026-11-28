package org.hameed.hameedmoneycomputer.ingestion;

public record RuleCreateDto(
        String matchPattern,
        Long targetAccountId
) {
}
