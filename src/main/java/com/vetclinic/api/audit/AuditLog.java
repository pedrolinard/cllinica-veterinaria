package com.vetclinic.api.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro append-only de ações sensíveis (quem fez o quê, quando).
 * Não é um histórico completo de todo campo alterado — cobre criação/edição/
 * exclusão dos tipos de dado onde essa rastreabilidade importa de verdade
 * (contas de funcionário, agenda, prontuários).
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    @Column(length = 500)
    private String detail;
}
