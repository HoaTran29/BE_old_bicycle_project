package com.backend.old_bicycle_project.specification;

import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
import org.springframework.data.jpa.domain.Specification;

public final class PayoutSpecification {

    private PayoutSpecification() {
    }

    public static Specification<Payout> fromAdminFilter(String keyword, PayoutType type, PayoutStatus status) {
        return Specification.where(matchesKeyword(keyword))
                .and(matchesType(type))
                .and(matchesStatus(status));
    }

    private static Specification<Payout> matchesKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            var recipient = root.join("recipient");
            var order = root.join("order", jakarta.persistence.criteria.JoinType.LEFT);
            var product = order.join("product", jakarta.persistence.criteria.JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(cb.coalesce(recipient.get("email"), "")), normalizedKeyword),
                    cb.like(cb.lower(cb.coalesce(recipient.get("firstName"), "")), normalizedKeyword),
                    cb.like(cb.lower(cb.coalesce(recipient.get("lastName"), "")), normalizedKeyword),
                    cb.like(cb.lower(cb.coalesce(root.get("accountNumber"), "")), normalizedKeyword),
                    cb.like(cb.lower(cb.coalesce(root.get("accountName"), "")), normalizedKeyword),
                    cb.like(cb.lower(cb.coalesce(root.get("transferContent"), "")), normalizedKeyword),
                    cb.like(cb.lower(cb.coalesce(root.get("bankReference"), "")), normalizedKeyword),
                    cb.like(cb.lower(cb.coalesce(product.get("title"), "")), normalizedKeyword)
            );
        };
    }

    private static Specification<Payout> matchesType(PayoutType type) {
        if (type == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    private static Specification<Payout> matchesStatus(PayoutStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
