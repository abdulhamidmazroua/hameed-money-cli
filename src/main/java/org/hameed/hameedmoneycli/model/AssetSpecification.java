package org.hameed.hameedmoneycli.model;

import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.springframework.data.jpa.domain.Specification;

public class AssetSpecification {

    public static Specification<Asset> hasNameOrSymbolContaining(String keyword) {
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

    public static Specification<Asset> hasCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.isBlank()) {
                return null;
            }
            return cb.equal(root.get("category"), AssetCategory.fromString(category.trim()));
        };
    }

    public static Specification<Asset> hasTradable(Boolean tradable) {
        return (root, query, cb) -> tradable == null ? null : cb.equal(root.get("isTradable"), tradable);
    }
}
