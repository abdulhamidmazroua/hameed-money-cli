package org.hameed.hameedmoneycli.model.dto;

public record RuleCreateDto(
        String matchPattern,
        Long targetAccountId
) {
}
