package org.hameed.hameedmoneycomputer.store;

import org.hameed.hameedmoneycomputer.ledger.Account;
import org.hameed.hameedmoneycomputer.ledger.AccountFilter;
import org.hameed.hameedmoneycomputer.ledger.AccountType;
import org.springframework.data.jpa.domain.Specification;

final class AccountSpecification {

    private AccountSpecification() {
    }

    static Specification<Account> matching(AccountFilter filter) {
        return Specification.where(hasNameContaining(filter.keyword()))
                .and(hasType(filter.type()))
                .and(hasAssetSymbol(filter.assetSymbol()));
    }

    private static Specification<Account> hasNameContaining(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("name")), pattern);
        };
    }

    private static Specification<Account> hasType(AccountType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("masterType"), type);
    }

    private static Specification<Account> hasAssetSymbol(String assetSymbol) {
        return (root, query, cb) -> {
            if (assetSymbol == null || assetSymbol.isBlank()) {
                return null;
            }
            return cb.equal(root.get("asset").get("symbol"), assetSymbol.trim().toUpperCase());
        };
    }
}
