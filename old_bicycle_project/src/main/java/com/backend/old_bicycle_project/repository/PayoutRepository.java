package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID>, JpaSpecificationExecutor<Payout> {

    @Override
    @EntityGraph(attributePaths = {
            "recipient",
            "order",
            "order.product",
            "order.buyer",
            "order.seller",
            "refundRequest",
            "completedBy"
    })
    Page<Payout> findAll(Specification<Payout> spec, Pageable pageable);

    Optional<Payout> findByRefundRequestId(UUID refundRequestId);

    Optional<Payout> findByOrderIdAndType(UUID orderId, PayoutType type);

    @EntityGraph(attributePaths = {"recipient", "order", "refundRequest"})
    List<Payout> findByRecipientIdAndStatusInOrderByCreatedAtAsc(UUID recipientId, List<PayoutStatus> statuses);

    long countByStatus(PayoutStatus status);
}
