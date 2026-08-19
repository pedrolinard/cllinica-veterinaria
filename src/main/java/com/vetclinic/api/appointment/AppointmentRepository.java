package com.vetclinic.api.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Page<Appointment> findByVetId(UUID vetId, Pageable pageable);

    Page<Appointment> findByPetId(UUID petId, Pageable pageable);

    /**
     * Busca os agendamentos ativos (não cancelados) de um veterinário dentro de uma
     * janela de tempo — usado para checar conflito de horário antes de criar/editar
     * um agendamento.
     */
    List<Appointment> findByVetIdAndStatusNotAndScheduledAtBetween(
            UUID vetId, AppointmentStatus excludedStatus, LocalDateTime from, LocalDateTime to
    );

    long countByPetId(UUID petId);

    long countByVetId(UUID vetId);
}
