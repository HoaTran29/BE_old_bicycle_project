package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("SELECT c FROM Conversation c WHERE c.buyer.id = :userId OR c.seller.id = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findConversationsByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.buyer.id = :userId OR c.seller.id = :userId")
    long countConversationsByUserId(@Param("userId") UUID userId);

    Optional<Conversation> findFirstByProductIdAndBuyerIdAndSellerIdOrderByCreatedAtAsc(
            UUID productId,
            UUID buyerId,
            UUID sellerId
    );
}
