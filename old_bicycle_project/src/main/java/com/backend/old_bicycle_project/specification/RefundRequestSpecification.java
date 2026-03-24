package com.backend.old_bicycle_project.specification;

import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class RefundRequestSpecification {

    private RefundRequestSpecification() {
    }

    public static Specification<RefundRequest> fromAdminFilter(String keyword, RefundStatus status) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            Join<RefundRequest, Order> orderJoin = root.join("order");
            Join<Order, Product> productJoin = orderJoin.join("product");
            Join<Order, User> buyerJoin = orderJoin.join("buyer");
            Join<Order, User> sellerJoin = orderJoin.join("seller");
            Join<RefundRequest, User> requesterJoin = root.join("requester");

            if (keyword != null && !keyword.isBlank()) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(productJoin.get("title"), "")), normalizedKeyword),
                        cb.like(cb.lower(cb.coalesce(requesterJoin.get("email"), "")), normalizedKeyword),
                        cb.like(cb.lower(cb.coalesce(requesterJoin.get("firstName"), "")), normalizedKeyword),
                        cb.like(cb.lower(cb.coalesce(requesterJoin.get("lastName"), "")), normalizedKeyword),
                        cb.like(cb.lower(cb.coalesce(buyerJoin.get("firstName"), "")), normalizedKeyword),
                        cb.like(cb.lower(cb.coalesce(buyerJoin.get("lastName"), "")), normalizedKeyword),
                        cb.like(cb.lower(cb.coalesce(sellerJoin.get("firstName"), "")), normalizedKeyword),
                        cb.like(cb.lower(cb.coalesce(sellerJoin.get("lastName"), "")), normalizedKeyword),
                        cb.like(cb.lower(root.get("reason")), normalizedKeyword),
                        cb.like(cb.lower(root.get("id").as(String.class)), normalizedKeyword),
                        cb.like(cb.lower(orderJoin.get("id").as(String.class)), normalizedKeyword)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
