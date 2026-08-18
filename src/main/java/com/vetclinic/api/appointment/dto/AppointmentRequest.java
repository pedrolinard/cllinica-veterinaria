package com.vetclinic.api.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentRequest(
        @NotNull(message = "Data/hora da consulta é obrigatória.")
        @Future(message = "A consulta deve ser marcada para uma data futura.")
        LocalDateTime scheduledAt,

        @Positive(message = "Duração deve ser positiva (em minutos).")
        Integer durationMin,

        String reason,

        @NotNull(message = "Pet é obrigatório.")
        UUID petId,

        @NotNull(message = "Veterinário é obrigatório.")
        UUID vetId
) {
}
