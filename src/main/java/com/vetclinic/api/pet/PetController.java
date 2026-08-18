package com.vetclinic.api.pet;

import com.vetclinic.api.pet.dto.PetRequest;
import com.vetclinic.api.pet.dto.PetResponse;
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
 * CRUD de pets. Leitura liberada para qualquer funcionário autenticado;
 * escrita restrita a ADMIN e RECEPTIONIST (cadastro/atualização de dados
 * cadastrais — diferente do prontuário médico, que é responsabilidade do VET).
 */
@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pets", description = "Cadastro de pets")
public class PetController {

    private final PetService petService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Cadastra um novo pet vinculado a um cliente")
    public ResponseEntity<PetResponse> create(@Valid @RequestBody PetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.create(request));
    }

    @GetMapping
    @Operation(summary = "Lista pets de forma paginada, com filtro opcional por cliente")
    public ResponseEntity<Page<PetResponse>> findAll(
            @RequestParam(required = false) UUID clientId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(petService.findAll(clientId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um pet pelo id")
    public ResponseEntity<PetResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(petService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Atualiza os dados cadastrais de um pet")
    public ResponseEntity<PetResponse> update(@PathVariable UUID id, @Valid @RequestBody PetRequest request) {
        return ResponseEntity.ok(petService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Remove um pet")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        petService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
