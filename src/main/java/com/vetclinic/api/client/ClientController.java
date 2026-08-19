package com.vetclinic.api.client;

import com.vetclinic.api.client.dto.ClientRequest;
import com.vetclinic.api.client.dto.ClientResponse;
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
 * CRUD de clientes (tutores). Leitura liberada para qualquer funcionário
 * autenticado; escrita restrita a ADMIN e RECEPTIONIST.
 */
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Clients", description = "Cadastro de clientes (tutores)")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Cadastra um novo cliente")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }

    @GetMapping
    @Operation(summary = "Lista clientes de forma paginada, com busca opcional por nome")
    public ResponseEntity<Page<ClientResponse>> findAll(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(clientService.findAll(q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cliente pelo id")
    public ResponseEntity<ClientResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Atualiza os dados de um cliente")
    public ResponseEntity<ClientResponse> update(@PathVariable UUID id, @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(clientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Remove um cliente (e seus pets, em cascata)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
