package com.backend.old_bicycle_project.specification;

import com.backend.old_bicycle_project.entity.Inspection;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class InspectionSpecification {

    private InspectionSpecification() {
    }

    public static Specification<Inspection> fromHistoryFilter(UUID inspectorId, String keyword) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("inspector")));
            predicates.add(cb.isNull(root.get("product").get("deletedAt")));

            if (inspectorId != null) {
                predicates.add(cb.equal(root.get("inspector").get("id"), inspectorId));
            }

            if (keyword != null && !keyword.isBlank()) {
                String normalizedKeyword = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("product").get("title")), normalizedKeyword),
                        cb.like(cb.lower(root.get("product").get("seller").get("firstName")), normalizedKeyword),
                        cb.like(cb.lower(root.get("product").get("seller").get("lastName")), normalizedKeyword),
                        cb.like(cb.lower(root.get("product").get("seller").get("email")), normalizedKeyword)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
