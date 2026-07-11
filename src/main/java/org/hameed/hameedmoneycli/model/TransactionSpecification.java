package org.hameed.hameedmoneycli.model;

import org.hameed.hameedmoneycli.enums.TransactionType;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import static java.time.temporal.ChronoUnit.DAYS;

public class TransactionSpecification {

    public static Specification<Transaction> hasTransactionType(String transactionType) {
        return (root, query, cb) -> {
            if (transactionType == null || transactionType.isBlank()) {
                return null;
            }
            return cb.equal(root.get("type"), TransactionType.fromString(transactionType.trim()));
        };
    }

    public static Specification<Transaction> hasFromAccountId(Long fromAccountId) {
        return (root, query, criteriaBuilder) -> fromAccountId == null ? null : criteriaBuilder.equal(root.get("fromAccount").get("id"), fromAccountId);
    }

    public static Specification<Transaction> hasToAccountId(Long toAccountId) {
        return (root, query, criteriaBuilder) -> toAccountId == null ? null : criteriaBuilder.equal(root.get("toAccount").get("id"), toAccountId);
    }

    public static Specification<Transaction> hasTransactionDateTimeFrom(String transactionDateTimeFrom) {
        return (root, query, cb) -> {
            if (transactionDateTimeFrom == null || transactionDateTimeFrom.isBlank()) {
                return null;
            }
            Long from = parseFilterStart(transactionDateTimeFrom);
            return cb.greaterThanOrEqualTo(root.get("transactionDate"), from);
        };
    }

    public static Specification<Transaction> hasTransactionDateTimeTo(String transactionDateTimeTo) {
        return (root, query, cb) -> {
            if (transactionDateTimeTo == null || transactionDateTimeTo.isBlank()) {
                return null;
            }
            String raw = transactionDateTimeTo.trim();
            if (raw.contains("T")) {
                Long end = Instant.parse(raw).toEpochMilli();
                return cb.lessThanOrEqualTo(root.get("transactionDate"), end);
            }
            LocalDate d = LocalDate.parse(raw);
            Long endExclusive = d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            return cb.lessThan(root.get("transactionDate"), endExclusive);
        };
    }

    public static Specification<Transaction> hasDescriptionContaining(String description) {
        return (root, query, cb) -> {
            if (description == null || description.isBlank()) {
                return null;
            }
            String pattern = "%" + description.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("description")), pattern);
        };
    }

    public static Specification<Transaction> hasAmountBetween(BigDecimal minAmount, BigDecimal maxAmount) {
        return (root, query, cb) -> {
            if (minAmount == null && maxAmount == null) {
                return null;
            }
            Specification<Transaction> fromSpec = (r, q, c) -> {
                var pred = c.conjunction();
                if (minAmount != null) {
                    pred = c.and(pred, c.greaterThanOrEqualTo(r.get("fromAmount"), minAmount));
                }
                if (maxAmount != null) {
                    pred = c.and(pred, c.lessThanOrEqualTo(r.get("fromAmount"), maxAmount));
                }
                return pred;
            };
            Specification<Transaction> toSpec = (r, q, c) -> {
                var pred = c.conjunction();
                if (minAmount != null) {
                    pred = c.and(pred, c.greaterThanOrEqualTo(r.get("toAmount"), minAmount));
                }
                if (maxAmount != null) {
                    pred = c.and(pred, c.lessThanOrEqualTo(r.get("toAmount"), maxAmount));
                }
                return pred;
            };
            return cb.or(fromSpec.toPredicate(root, query, cb), toSpec.toPredicate(root, query, cb));
        };
    }

    public static Specification<Transaction> hasInvolvedAccount(Long accountId) {
        return (root, query, cb) -> {
            if (accountId == null) {
                return null;
            }
            return cb.or(
                    cb.equal(root.get("fromAccount").get("id"), accountId),
                    cb.equal(root.get("toAccount").get("id"), accountId)
            );
        };
    }

    private static Long parseFilterStart(String raw) {
        try {
            if (raw.contains("T")) {
                return Instant.parse(raw).toEpochMilli();
            }
            LocalDate d = LocalDate.parse(raw);
            return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid start date: " + raw, e);
        }
    }

}
