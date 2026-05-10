package org.hameed.hameedmoneycli.model;

import org.hameed.hameedmoneycli.enums.TransactionType;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public class TransactionSpecification {

    public static Specification<Transaction> hasTransactionType(String transactionType) {
        return (root, query, cb) -> {
            if (transactionType == null || transactionType.isBlank()) {
                return null;
            }
            return cb.equal(root.get("type"), TransactionType.valueOf(transactionType.trim()));
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
            Instant from = parseFilterStart(transactionDateTimeFrom);
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
                Instant end = Instant.parse(raw);
                return cb.lessThanOrEqualTo(root.get("transactionDate"), end);
            }
            LocalDate d = LocalDate.parse(raw);
            Instant endExclusive = d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            return cb.lessThan(root.get("transactionDate"), endExclusive);
        };
    }

    private static Instant parseFilterStart(String raw) {
        try {
            if (raw.contains("T")) {
                return Instant.parse(raw);
            }
            LocalDate d = LocalDate.parse(raw);
            return d.atStartOfDay(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid start date: " + raw, e);
        }
    }

}
