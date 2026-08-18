package com.vetclinic.api.service;

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
        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.of("Serviço", id);
        }
        repository.deleteById(id);
    }

    private ClinicService getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Serviço", id));
    }
}
