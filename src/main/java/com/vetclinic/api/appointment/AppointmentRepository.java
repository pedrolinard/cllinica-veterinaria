package com.vetclinic.api.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    interface StatusCount {
        AppointmentStatus getStatus();
        long getCount();
    }

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

    @Query("select a.status as status, count(a) as count from Appointment a group by a.status")
    List<StatusCount> countGroupedByStatus();

    /** Agendamentos ativos (não cancelados) de um dia específico, para o relatório de agenda. */
    List<Appointment> findByStatusNotAndScheduledAtBetweenOrderByScheduledAt(
            AppointmentStatus excludedStatus, LocalDateTime from, LocalDateTime to
    );

    /** Consultas concluídas num período, para o relatório de faturamento por serviço. */
    List<Appointment> findByStatusAndScheduledAtBetween(
            AppointmentStatus status, LocalDateTime from, LocalDateTime to
    );
}
