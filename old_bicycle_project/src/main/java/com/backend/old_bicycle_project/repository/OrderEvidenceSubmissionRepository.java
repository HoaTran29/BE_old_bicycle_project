package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.OrderEvidenceSubmission;
import com.backend.old_bicycle_project.entity.enums.OrderEvidenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderEvidenceSubmissionRepository extends JpaRepository<OrderEvidenceSubmission, UUID> {

    boolean existsByOrderIdAndEvidenceType(UUID orderId, OrderEvidenceType evidenceType);

    @Query("""
            select distinct submission
            from OrderEvidenceSubmission submission
            left join fetch submission.submittedByUser
            left join fetch submission.files
            where submission.order.id in :orderIds
            """)
    List<OrderEvidenceSubmission> findDetailedByOrderIds(@Param("orderIds") Collection<UUID> orderIds);

    @Query("""
            select distinct submission
            from OrderEvidenceSubmission submission
            left join fetch submission.submittedByUser
            left join fetch submission.files
            where submission.order.id = :orderId
              and submission.evidenceType = :evidenceType
            """)
    Optional<OrderEvidenceSubmission> findDetailedByOrderIdAndEvidenceType(
            @Param("orderId") UUID orderId,
            @Param("evidenceType") OrderEvidenceType evidenceType
    );
}
