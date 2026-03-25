package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, UUID> {
}
