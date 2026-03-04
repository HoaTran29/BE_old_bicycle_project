package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.FrameMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FrameMaterialRepository extends JpaRepository<FrameMaterial, UUID> {
}
