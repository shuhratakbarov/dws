package com.fintech.walletservice.repository;

import com.fintech.walletservice.domain.ReconciliationAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReconciliationAuditRepository extends JpaRepository<ReconciliationAudit, UUID> {

    /**
     * Find all mismatches for a specific wallet
     */
    List<ReconciliationAudit> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    /**
     * Find unresolved mismatches (for ops team dashboard)
     */
    Page<ReconciliationAudit> findByStatusOrderByCreatedAtDesc(
            ReconciliationAudit.ReconciliationStatus status,
            Pageable pageable
    );

    /**
     * Count unresolved mismatches (for alerting)
     */
    long countByStatus(ReconciliationAudit.ReconciliationStatus status);
}

