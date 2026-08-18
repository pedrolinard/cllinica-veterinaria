package com.vetclinic.api.appointment.dto;

import com.vetclinic.api.appointment.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "Status é obrigatório.")
        AppointmentStatus status
) {
}
