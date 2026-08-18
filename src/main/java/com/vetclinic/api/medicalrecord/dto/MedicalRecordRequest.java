package com.vetclinic.api.medicalrecord.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record MedicalRecordRequest(
        @NotNull(message = "Consulta (appointmentId) é obrigatória.")
        UUID appointmentId,

        String diagnosis,

        String notes,

        @Positive(message = "Peso deve ser positivo.")
        Double weightKg,

        String vaccines,

        String prescriptions
) {
}
