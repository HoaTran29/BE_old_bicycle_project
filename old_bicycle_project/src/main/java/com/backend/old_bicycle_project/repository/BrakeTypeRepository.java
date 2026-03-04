package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.BrakeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BrakeTypeRepository extends JpaRepository<BrakeType, UUID> {
}
