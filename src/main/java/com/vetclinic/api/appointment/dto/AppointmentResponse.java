package com.vetclinic.api.appointment.dto;

import com.vetclinic.api.appointment.Appointment;
import com.vetclinic.api.appointment.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        LocalDateTime scheduledAt,
        LocalDateTime endsAt,
        Integer durationMin,
        AppointmentStatus status,
        String reason,
        UUID petId,
        String petName,
        UUID vetId,
        String vetName
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getScheduledAt(),
                appointment.getEndsAt(),
                appointment.getDurationMin(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getPet().getId(),
                appointment.getPet().getName(),
                appointment.getVet().getId(),
                appointment.getVet().getName()
        );
    }
}
