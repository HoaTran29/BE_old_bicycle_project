package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, UUID> {
    
    Optional<Inspection> findByProductId(UUID productId);
    
    boolean existsByProductId(UUID productId);
}
