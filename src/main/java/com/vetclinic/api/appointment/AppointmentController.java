package com.vetclinic.api.appointment;

import com.vetclinic.api.appointment.dto.AppointmentRequest;
import com.vetclinic.api.appointment.dto.AppointmentResponse;
import com.vetclinic.api.appointment.dto.UpdateStatusRequest;
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
 * Agendamento de consultas. Criação/edição restrita a ADMIN e RECEPTIONIST
 * (quem organiza a agenda); qualquer funcionário autenticado pode consultar.
 * A mudança de status (ex: marcar como CONCLUÍDA) também é permitida ao VET,
 * que é quem efetivamente realiza o atendimento.
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Appointments", description = "Agendamento de consultas")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Agenda uma nova consulta (valida conflito de horário do veterinário)")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.create(request));
    }

    @GetMapping
    @Operation(summary = "Lista consultas, com filtro opcional por veterinário ou pet")
    public ResponseEntity<Page<AppointmentResponse>> findAll(
            @RequestParam(required = false) UUID vetId,
            @RequestParam(required = false) UUID petId,
            @PageableDefault(size = 20, sort = "scheduledAt") Pageable pageable
    ) {
        return ResponseEntity.ok(appointmentService.findAll(vetId, petId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma consulta pelo id")
    public ResponseEntity<AppointmentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Reagenda uma consulta (data, duração, pet ou veterinário)")
    public ResponseEntity<AppointmentResponse> reschedule(
            @PathVariable UUID id, @Valid @RequestBody AppointmentRequest request
    ) {
        return ResponseEntity.ok(appointmentService.reschedule(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'VET')")
    @Operation(summary = "Atualiza o status da consulta (confirmar, concluir, cancelar)")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Remove uma consulta")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        appointmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
