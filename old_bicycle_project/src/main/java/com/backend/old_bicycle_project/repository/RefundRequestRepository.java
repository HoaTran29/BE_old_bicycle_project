package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID>, JpaSpecificationExecutor<RefundRequest> {

    @Override
    @EntityGraph(attributePaths = {
            "order",
            "order.buyer",
            "order.seller",
            "order.product",
            "payment",
            "requester",
            "reviewedBy"
    })
    Page<RefundRequest> findAll(Specification<RefundRequest> spec, Pageable pageable);

    Optional<RefundRequest> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(UUID orderId, RefundStatus status);

    Optional<RefundRequest> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);

    long countByStatus(RefundStatus status);
}
