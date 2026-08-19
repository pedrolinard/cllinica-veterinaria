package com.vetclinic.api.audit;

import com.vetclinic.api.audit.dto.AuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit Log", description = "Trilha de auditoria de ações sensíveis (somente ADMIN)")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Lista o log de auditoria, com filtro opcional por tipo de entidade")
    public ResponseEntity<Page<AuditLogResponse>> findAll(
            @RequestParam(required = false) String entityType,
            @PageableDefault(size = 30) Pageable pageable
    ) {
        return ResponseEntity.ok(auditService.findAll(entityType, pageable));
    }
}
