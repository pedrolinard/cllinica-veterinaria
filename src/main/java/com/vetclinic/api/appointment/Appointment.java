package com.vetclinic.api.appointment;

import com.vetclinic.api.common.BaseEntity;
import com.vetclinic.api.pet.Pet;
import com.vetclinic.api.service.ClinicService;
import com.vetclinic.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Consulta agendada de um pet com um veterinário.
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Appointment extends BaseEntity {

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "duration_min", nullable = false)
    @Builder.Default
    private Integer durationMin = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vet_id", nullable = false)
    private User vet;

    /** Serviço do catálogo realizado nesta consulta (opcional; usado no relatório de faturamento). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ClinicService service;

    /** Horário de término calculado a partir de scheduledAt + durationMin. */
    public LocalDateTime getEndsAt() {
        return scheduledAt.plusMinutes(durationMin);
    }

    /** Verifica se este agendamento se sobrepõe a outro intervalo [start, end). */
    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return scheduledAt.isBefore(end) && start.isBefore(getEndsAt());
    }
}
