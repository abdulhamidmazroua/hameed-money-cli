package org.hameed.hameedmoneycomputer.store;

import org.hameed.hameedmoneycomputer.market.Asset;
import org.hameed.hameedmoneycomputer.market.AssetCategory;
import org.hameed.hameedmoneycomputer.market.AssetFilter;
import org.springframework.data.jpa.domain.Specification;

final class AssetSpecification {

    private AssetSpecification() {
    }

    static Specification<Asset> matching(AssetFilter filter) {
        return Specification.where(hasNameOrSymbolContaining(filter.keyword()))
                .and(hasCategory(filter.category()))
                .and(hasTradable(filter.tradable()));
    }

    private static Specification<Asset> hasNameOrSymbolContaining(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("symbol")), pattern)
            );
        };
    }

    private static Specification<Asset> hasCategory(AssetCategory category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    private static Specification<Asset> hasTradable(Boolean tradable) {
        return (root, query, cb) -> tradable == null ? null : cb.equal(root.get("isTradable"), tradable);
    }
}
