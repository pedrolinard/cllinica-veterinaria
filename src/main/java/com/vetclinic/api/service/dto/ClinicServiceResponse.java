package com.vetclinic.api.service.dto;

import com.vetclinic.api.service.ClinicService;

import java.util.UUID;

public record ClinicServiceResponse(
        UUID id,
        String name,
        String description,
        Integer priceCents,
        Integer durationMin,
        Boolean active
) {
    public static ClinicServiceResponse from(ClinicService service) {
        return new ClinicServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPriceCents(),
                service.getDurationMin(),
                service.getActive()
        );
    }
}
