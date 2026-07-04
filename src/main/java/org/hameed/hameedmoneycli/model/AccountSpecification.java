package org.hameed.hameedmoneycli.model;

import org.hameed.hameedmoneycli.enums.AccountType;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.springframework.data.jpa.domain.Specification;

public class AccountSpecification {

    public static Specification<Account> hasNameContaining(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("name")), pattern);
        };
    }

    public static Specification<Account> hasMasterType(String masterType) {
        return (root, query, cb) -> {
            if (masterType == null || masterType.isBlank()) {
                return null;
            }
            return cb.equal(root.get("masterType"), AccountType.fromString(masterType.trim()));
        };
    }

    public static Specification<Account> hasAssetSymbol(String assetSymbol) {
        return (root, query, cb) -> {
            if (assetSymbol == null || assetSymbol.isBlank()) {
                return null;
            }
            return cb.equal(root.get("asset").get("symbol"), assetSymbol.trim().toUpperCase());
        };
    }
}
