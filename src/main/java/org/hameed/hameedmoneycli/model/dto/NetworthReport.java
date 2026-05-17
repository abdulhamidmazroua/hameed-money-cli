package org.hameed.hameedmoneycli.model.dto;

import java.math.BigDecimal;

public record NetworthReport(
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal netWorth
) implements Report {
}
