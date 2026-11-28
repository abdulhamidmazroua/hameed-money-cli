package org.hameed.hameedmoneycomputer.store;

import org.hameed.hameedmoneycomputer.ledger.Transaction;
import org.hameed.hameedmoneycomputer.ledger.TransactionFilter;
import org.hameed.hameedmoneycomputer.ledger.TransactionType;
import org.hameed.hameedmoneycomputer.shared.DateUtil;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

final class TransactionSpecification {

    private TransactionSpecification() {
    }

    static Specification<Transaction> matching(TransactionFilter filter) {
        return Specification.where(hasType(filter.type()))
                .and(hasAccountId("fromAccount", filter.fromAccountId()))
                .and(hasAccountId("toAccount", filter.toAccountId()))
                .and(onOrAfter(filter.dateFrom()))
                .and(onOrBefore(filter.dateTo()))
                .and(hasDescriptionContaining(filter.description()))
                .and(hasAmountBetween(filter.minAmount(), filter.maxAmount()))
                .and(involvesAccount(filter.involvedAccountId()));
    }

    private static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    private static Specification<Transaction> hasAccountId(String side, Long accountId) {
        return (root, query, cb) -> accountId == null ? null : cb.equal(root.get(side).get("id"), accountId);
    }

    private static Specification<Transaction> onOrAfter(LocalDate date) {
        return (root, query, cb) -> date == null ? null
                : cb.greaterThanOrEqualTo(root.get("transactionDate"), DateUtil.startOfDayMillis(date));
    }

    private static Specification<Transaction> onOrBefore(LocalDate date) {
        return (root, query, cb) -> date == null ? null
                : cb.lessThan(root.get("transactionDate"), DateUtil.startOfDayMillis(date.plusDays(1)));
    }

    private static Specification<Transaction> hasDescriptionContaining(String description) {
        return (root, query, cb) -> {
            if (description == null || description.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("description")), "%" + description.trim().toLowerCase() + "%");
        };
    }

    /** Matches when either side's amount lies in [min, max]. */
    private static Specification<Transaction> hasAmountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) {
                return null;
            }
            Predicate from = cb.conjunction();
            Predicate to = cb.conjunction();
            if (min != null) {
                from = cb.and(from, cb.greaterThanOrEqualTo(root.get("fromAmount"), min));
                to = cb.and(to, cb.greaterThanOrEqualTo(root.get("toAmount"), min));
            }
            if (max != null) {
                from = cb.and(from, cb.lessThanOrEqualTo(root.get("fromAmount"), max));
                to = cb.and(to, cb.lessThanOrEqualTo(root.get("toAmount"), max));
            }
            return cb.or(from, to);
        };
    }

    private static Specification<Transaction> involvesAccount(Long accountId) {
        return (root, query, cb) -> accountId == null ? null : cb.or(
                cb.equal(root.get("fromAccount").get("id"), accountId),
                cb.equal(root.get("toAccount").get("id"), accountId));
    }
}
