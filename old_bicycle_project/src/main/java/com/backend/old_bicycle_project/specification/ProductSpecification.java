package com.backend.old_bicycle_project.specification;

import com.backend.old_bicycle_project.dto.product.ProductFilterRequest;
import com.backend.old_bicycle_project.entity.Inspection;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductSpecification {

    private static final List<ProductStatus> PUBLIC_VISIBLE_STATUSES = List.of(
            ProductStatus.active,
            ProductStatus.pending_inspection,
            ProductStatus.inspected_passed,
            ProductStatus.inspected_failed
    );

    public static Specification<Product> fromFilter(ProductFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(root.get("status").in(PUBLIC_VISIBLE_STATUSES));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("title")),
                        "%" + filter.getKeyword().toLowerCase() + "%"
                ));
            }

            if (filter.getBrandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), filter.getBrandId()));
            }

            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
            }

            if (filter.getBrakeTypeId() != null) {
                predicates.add(cb.equal(root.get("brakeType").get("id"), filter.getBrakeTypeId()));
            }

            if (filter.getFrameMaterialId() != null) {
                predicates.add(cb.equal(root.get("frameMaterial").get("id"), filter.getFrameMaterialId()));
            }

            if (filter.getCondition() != null) {
                predicates.add(cb.equal(root.get("condition"), filter.getCondition()));
            }

            if (filter.getFrameSize() != null && !filter.getFrameSize().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("frameSize")), filter.getFrameSize().toLowerCase()));
            }

            if (filter.getWheelSize() != null && !filter.getWheelSize().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("wheelSize")), filter.getWheelSize().toLowerCase()));
            }

            if (filter.getGroupset() != null && !filter.getGroupset().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("groupset")),
                        "%" + filter.getGroupset().toLowerCase() + "%"
                ));
            }

            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }

            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }

            if (filter.getProvince() != null && !filter.getProvince().isBlank()) {
                predicates.add(cb.equal(root.get("province"), filter.getProvince()));
            }

            if (Boolean.TRUE.equals(filter.getHasInspection())) {
                Subquery<Long> inspectionSubquery = query.subquery(Long.class);
                Root<Inspection> inspectionRoot = inspectionSubquery.from(Inspection.class);
                inspectionSubquery.select(cb.literal(1L))
                        .where(
                                cb.equal(inspectionRoot.get("product"), root),
                                cb.isTrue(inspectionRoot.get("passed")),
                                cb.greaterThan(inspectionRoot.get("validUntil"), LocalDateTime.now())
                        );
                predicates.add(cb.exists(inspectionSubquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> withStatus(ProductStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Product> fromAdminFilter(ProductStatus status, UUID sellerId, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (sellerId != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), sellerId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String normalizedKeyword = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), normalizedKeyword),
                        cb.like(cb.lower(root.get("description")), normalizedKeyword),
                        cb.like(cb.lower(root.get("seller").get("email")), normalizedKeyword),
                        cb.like(cb.lower(root.get("seller").get("firstName")), normalizedKeyword),
                        cb.like(cb.lower(root.get("seller").get("lastName")), normalizedKeyword)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
