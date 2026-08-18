package com.vetclinic.api.medicalrecord;

import com.vetclinic.api.medicalrecord.dto.MedicalRecordRequest;
import com.vetclinic.api.medicalrecord.dto.MedicalRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Prontuários médicos. Criação/edição restrita a VET e ADMIN;
 * leitura liberada para qualquer funcionário autenticado.
 */
@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Medical Records", description = "Prontuários médicos dos pets")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping
    @PreAuthorize("hasAnyRole('VET', 'ADMIN')")
    @Operation(summary = "Registra o prontuário de uma consulta concluída")
    public ResponseEntity<MedicalRecordResponse> create(@Valid @RequestBody MedicalRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicalRecordService.create(request));
    }

    @GetMapping
    @Operation(summary = "Lista o histórico de prontuários de um pet")
    public ResponseEntity<Page<MedicalRecordResponse>> findByPet(
            @RequestParam UUID petId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(medicalRecordService.findByPet(petId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um prontuário pelo id")
    public ResponseEntity<MedicalRecordResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(medicalRecordService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VET', 'ADMIN')")
    @Operation(summary = "Atualiza um prontuário")
    public ResponseEntity<MedicalRecordResponse> update(
            @PathVariable UUID id, @Valid @RequestBody MedicalRecordRequest request
    ) {
        return ResponseEntity.ok(medicalRecordService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove um prontuário (uso administrativo/correção de erros)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        medicalRecordService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
