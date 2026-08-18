package com.vetclinic.api.medicalrecord.dto;

import com.vetclinic.api.medicalrecord.MedicalRecord;

import java.time.Instant;
import java.util.UUID;

public record MedicalRecordResponse(
        UUID id,
        String diagnosis,
        String notes,
        Double weightKg,
        String vaccines,
        String prescriptions,
        UUID appointmentId,
        UUID petId,
        String petName,
        Instant createdAt
) {
    public static MedicalRecordResponse from(MedicalRecord record) {
        return new MedicalRecordResponse(
                record.getId(),
                record.getDiagnosis(),
                record.getNotes(),
                record.getWeightKg(),
                record.getVaccines(),
                record.getPrescriptions(),
                record.getAppointment().getId(),
                record.getPet().getId(),
                record.getPet().getName(),
                record.getCreatedAt()
        );
    }
}
