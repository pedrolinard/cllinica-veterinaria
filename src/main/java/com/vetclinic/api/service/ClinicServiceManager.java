package com.vetclinic.api.service;

import com.vetclinic.api.appointment.AppointmentRepository;
import com.vetclinic.api.audit.AuditAction;
import com.vetclinic.api.audit.AuditService;
import com.vetclinic.api.common.exception.ConflictException;
import com.vetclinic.api.common.exception.ResourceNotFoundException;
import com.vetclinic.api.service.dto.ClinicServiceRequest;
import com.vetclinic.api.service.dto.ClinicServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Camada de aplicação para o catálogo de serviços.
 * Nomeada "Manager" (em vez de "Service") para evitar confusão com o
 * conceito de entidade "ClinicService" neste mesmo pacote.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicServiceManager {

    private final ClinicServiceRepository repository;
    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;

    @Transactional
    public ClinicServiceResponse create(ClinicServiceRequest request) {
        ClinicService service = ClinicService.builder()
                .name(request.name())
                .description(request.description())
                .priceCents(request.priceCents())
                .durationMin(request.durationMin() != null ? request.durationMin() : 30)
                .active(request.active() == null || request.active())
                .build();

        return ClinicServiceResponse.from(repository.save(service));
    }

    public List<ClinicServiceResponse> findAll(boolean onlyActive) {
        List<ClinicService> services = onlyActive ? repository.findByActiveTrue() : repository.findAll();
        return services.stream().map(ClinicServiceResponse::from).toList();
    }

    public ClinicServiceResponse findById(UUID id) {
        return ClinicServiceResponse.from(getOrThrow(id));
    }

    @Transactional
    public ClinicServiceResponse update(UUID id, ClinicServiceRequest request) {
        ClinicService service = getOrThrow(id);
        service.setName(request.name());
        service.setDescription(request.description());
        service.setPriceCents(request.priceCents());
        service.setDurationMin(request.durationMin() != null ? request.durationMin() : service.getDurationMin());
        service.setActive(request.active() == null || request.active());
        return ClinicServiceResponse.from(repository.save(service));
    }

    @Transactional
    public void delete(UUID id) {
        ClinicService service = getOrThrow(id);

        long appointments = appointmentRepository.countByServiceId(id);
        if (appointments > 0) {
            throw new ConflictException(
                    "Não é possível excluir: este serviço está vinculado a " + appointments
                            + " consulta(s). Desative-o em vez de excluir, se não for mais oferecido."
            );
        }

        repository.delete(service);
        auditService.record("ClinicService", id, AuditAction.DELETE, "Serviço removido: " + service.getName());
    }

    public ClinicService getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Serviço", id));
    }
}
