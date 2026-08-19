package com.vetclinic.api.audit;

import com.vetclinic.api.audit.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(String entityType, UUID entityId, AuditAction action, String detail) {
        auditLogRepository.save(AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .detail(detail)
                .performedBy(currentUser())
                .build());
    }

    public Page<AuditLogResponse> findAll(String entityType, Pageable pageable) {
        Page<AuditLog> page = StringUtils.hasText(entityType)
                ? auditLogRepository.findByEntityTypeOrderByPerformedAtDesc(entityType, pageable)
                : auditLogRepository.findAllByOrderByPerformedAtDesc(pageable);
        return page.map(AuditLogResponse::from);
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "sistema";
        }
        return auth.getName();
    }
}
