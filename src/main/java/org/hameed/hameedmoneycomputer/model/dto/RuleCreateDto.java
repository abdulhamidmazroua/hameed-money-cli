package org.hameed.hameedmoneycomputer.model.dto;

public record RuleCreateDto(
        String matchPattern,
        Long targetAccountId
) {
}
