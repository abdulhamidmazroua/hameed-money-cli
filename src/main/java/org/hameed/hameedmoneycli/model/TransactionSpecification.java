package org.hameed.hameedmoneycli.model;

import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.springframework.data.jpa.domain.Specification;

public class TransactionSpecification {

    public static Specification<Transaction> hasTransactionType(String transactionType) {
        return (root, query, criteriaBuilder) -> transactionType == null ? null : criteriaBuilder.equal(root.get("transactionType"), transactionType);
    }

    public static Specification<Transaction> hasFromAccountId(Long fromAccountId) {
        return (root, query, criteriaBuilder) -> fromAccountId == null ? null : criteriaBuilder.equal(root.get("fromAccount").get("id"), fromAccountId);
    }

    public static Specification<Transaction> hasToAccountId(Long toAccountId) {
        return (root, query, criteriaBuilder) -> toAccountId == null ? null : criteriaBuilder.equal(root.get("toAccount").get("id"), toAccountId);
    }

    public static Specification<Transaction> hasTransactionDateTimeFrom(String transactionDateTimeFrom) {
        return (root, query, criteriaBuilder) -> transactionDateTimeFrom == null ? null : criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), transactionDateTimeFrom);
    }

    public static Specification<Transaction> hasTransactionDateTimeTo(String transactionDateTimeTo) {
        return (root, query, criteriaBuilder) -> transactionDateTimeTo == null ? null : criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), transactionDateTimeTo);
    }

}
