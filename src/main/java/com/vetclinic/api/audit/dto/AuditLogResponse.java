package com.vetclinic.api.audit.dto;

import com.vetclinic.api.audit.AuditAction;
import com.vetclinic.api.audit.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        Instant performedAt,
        String performedBy,
        String entityType,
        UUID entityId,
        AuditAction action,
        String detail
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getPerformedAt(),
                log.getPerformedBy(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getDetail()
        );
    }
}
