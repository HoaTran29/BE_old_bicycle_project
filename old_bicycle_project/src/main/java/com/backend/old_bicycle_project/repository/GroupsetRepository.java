package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Groupset;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupsetRepository extends JpaRepository<Groupset, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    Optional<Groupset> findByNameIgnoreCase(String name);

    default List<Groupset> findAllSortedByName() {
        return findAll(Sort.by(Sort.Direction.ASC, "name"));
    }
}
