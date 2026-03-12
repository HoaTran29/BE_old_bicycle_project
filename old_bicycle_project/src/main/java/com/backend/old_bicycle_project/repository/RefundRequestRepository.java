package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    Optional<RefundRequest> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(UUID orderId, RefundStatus status);
}
