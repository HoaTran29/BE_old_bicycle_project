package com.backend.old_bicycle_project.specification;

import com.backend.old_bicycle_project.dto.product.ProductFilterRequest;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> fromFilter(ProductFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Chỉ hiển thị sản phẩm active (trừ khi được gọi từ admin)
            predicates.add(cb.equal(root.get("status"), ProductStatus.active));

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // Tìm theo keyword trong title
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("title")),
                        "%" + filter.getKeyword().toLowerCase() + "%"
                ));
            }

            // Filter theo brandId
            if (filter.getBrandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), filter.getBrandId()));
            }

            // Filter theo categoryId
            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
            }

            // Filter theo brakeTypeId
            if (filter.getBrakeTypeId() != null) {
                predicates.add(cb.equal(root.get("brakeType").get("id"), filter.getBrakeTypeId()));
            }

            // Filter theo frameMaterialId
            if (filter.getFrameMaterialId() != null) {
                predicates.add(cb.equal(root.get("frameMaterial").get("id"), filter.getFrameMaterialId()));
            }

            // Filter theo condition
            if (filter.getCondition() != null) {
                predicates.add(cb.equal(root.get("condition"), filter.getCondition()));
            }

            // Filter theo khoảng giá
            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }

            // Filter theo tỉnh/thành
            if (filter.getProvince() != null && !filter.getProvince().isBlank()) {
                predicates.add(cb.equal(root.get("province"), filter.getProvince()));
            }

            // Filter xe có Verified Badge: sẽ được bổ sung khi có Inspection entity
            // if (Boolean.TRUE.equals(filter.getHasInspection())) { ... }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> withStatus(ProductStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }
}
