package com.vetclinic.api.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityTypeOrderByPerformedAtDesc(String entityType, Pageable pageable);

    Page<AuditLog> findAllByOrderByPerformedAtDesc(Pageable pageable);
}
