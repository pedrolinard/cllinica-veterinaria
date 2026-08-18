package com.vetclinic.api.service;

import com.vetclinic.api.service.dto.ClinicServiceRequest;
import com.vetclinic.api.service.dto.ClinicServiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Catálogo de serviços/produtos da clínica. Leitura liberada para qualquer
 * funcionário autenticado; escrita restrita a ADMIN.
 */
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Services", description = "Catálogo de serviços/produtos da clínica")
public class ClinicServiceController {

    private final ClinicServiceManager clinicServiceManager;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra um novo serviço no catálogo")
    public ResponseEntity<ClinicServiceResponse> create(@Valid @RequestBody ClinicServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicServiceManager.create(request));
    }

    @GetMapping
    @Operation(summary = "Lista os serviços do catálogo")
    public ResponseEntity<List<ClinicServiceResponse>> findAll(
            @RequestParam(name = "onlyActive", defaultValue = "false") boolean onlyActive
    ) {
        return ResponseEntity.ok(clinicServiceManager.findAll(onlyActive));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um serviço pelo id")
    public ResponseEntity<ClinicServiceResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(clinicServiceManager.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza um serviço do catálogo")
    public ResponseEntity<ClinicServiceResponse> update(
            @PathVariable UUID id, @Valid @RequestBody ClinicServiceRequest request
    ) {
        return ResponseEntity.ok(clinicServiceManager.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove um serviço do catálogo")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clinicServiceManager.delete(id);
        return ResponseEntity.noContent().build();
    }
}
