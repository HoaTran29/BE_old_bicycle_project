package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.entity.enums.PaymentPhase;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByGatewayOrderCode(String gatewayOrderCode);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    Optional<Payment> findFirstByOrderIdAndPhaseOrderByCreatedAtDesc(UUID orderId, PaymentPhase phase);

    boolean existsByOrderIdAndPhaseAndStatusIn(UUID orderId, PaymentPhase phase, Collection<PaymentStatus> statuses);
}
